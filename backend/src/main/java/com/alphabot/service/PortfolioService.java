package com.alphabot.service;

import com.alphabot.dto.TradeOrderRequest;
import com.alphabot.entity.*;
import com.alphabot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioPositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final StockQuoteSyncService stockQuoteService;

    private static final String DEFAULT_PORTFOLIO_NAME = "AI Auto Trader";
    private static final BigDecimal MAX_POSITION_SIZE_PERCENT = new BigDecimal("0.15"); // Max 15% equity per stock

    public Portfolio getDefaultPortfolio() {
        return portfolioRepository.findByName(DEFAULT_PORTFOLIO_NAME)
                .orElseThrow(() -> new RuntimeException("Default portfolio not found! Please run migrations."));
    }

    @Transactional
    public void executeTrade(TradeOrderRequest order) {
        Portfolio portfolio = getDefaultPortfolio();
        String ticker = order.getTicker().toUpperCase();

        Optional<StockQuote> latestQuoteOpt = stockQuoteService.getLatestQuote(ticker);
        if (latestQuoteOpt.isEmpty() || latestQuoteOpt.get().getMatchPrice() == null
                || latestQuoteOpt.get().getMatchPrice() == 0.0) {
            log.warn("Cannot execute trade for {}: No valid market price available.", ticker);
            return;
        }

        // Price is usually in thousands (e.g. 95.5 = 95,500 VND), but we match exactly
        // what is in DB
        BigDecimal currentPrice = BigDecimal.valueOf(latestQuoteOpt.get().getMatchPrice())
                .multiply(new BigDecimal("1000"));

        if ("BUY".equalsIgnoreCase(order.getAction())) {
            executeBuy(portfolio, ticker, currentPrice, order.getReason());
        } else if ("SELL".equalsIgnoreCase(order.getAction())) {
            executeSell(portfolio, ticker, currentPrice, order.getReason());
        }
    }

    private void executeBuy(Portfolio portfolio, String ticker, BigDecimal price, String reason) {
        // Calculate Total Equity = Cash + Stock Value
        BigDecimal totalEquity = calculateTotalEquity(portfolio);

        // Max money can allocate to this stock (15% of equity)
        BigDecimal maxAllocation = totalEquity.multiply(MAX_POSITION_SIZE_PERCENT);

        // Check if we already have a position
        Optional<PortfolioPosition> existingPosOpt = positionRepository.findByPortfolioIdAndTicker(portfolio.getId(),
                ticker);
        BigDecimal currentPositionValue = BigDecimal.ZERO;
        if (existingPosOpt.isPresent()) {
            currentPositionValue = new BigDecimal(existingPosOpt.get().getQuantity()).multiply(price);
        }

        // Remaining allowed allocation
        BigDecimal allowedToBuyValue = maxAllocation.subtract(currentPositionValue);

        if (allowedToBuyValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Skip BUY {}: Position size reached 15% limit constraint.", ticker);
            return;
        }

        // Check available cash
        BigDecimal actualBuyValue = allowedToBuyValue.min(portfolio.getCashBalance());

        // Calculate shares (lot 100 on HOSE normally, here we just do exact division
        // for paper trading,
        // to make it more realistic we floor to nearest 100)
        int sharesToBuy = actualBuyValue.divide(price, RoundingMode.DOWN).intValue();
        sharesToBuy = (sharesToBuy / 100) * 100;

        if (sharesToBuy <= 0) {
            log.info("Skip BUY {}: Not enough balance/allocation to buy at least 100 shares.", ticker);
            return;
        }

        BigDecimal cost = price.multiply(new BigDecimal(sharesToBuy));

        // Update Cash
        portfolio.setCashBalance(portfolio.getCashBalance().subtract(cost));
        portfolioRepository.save(portfolio);

        // Update Position
        PortfolioPosition position = existingPosOpt.orElse(new PortfolioPosition());
        if (position.getId() == null) {
            position.setPortfolio(portfolio);
            position.setTicker(ticker);
            position.setQuantity(sharesToBuy);
            position.setAveragePrice(price);
        } else {
            // Recalculate average price
            BigDecimal totalOldCost = position.getAveragePrice().multiply(new BigDecimal(position.getQuantity()));
            int newQuantity = position.getQuantity() + sharesToBuy;
            BigDecimal newAveragePrice = totalOldCost.add(cost).divide(new BigDecimal(newQuantity), 4,
                    RoundingMode.HALF_UP);
            position.setQuantity(newQuantity);
            position.setAveragePrice(newAveragePrice);
        }
        positionRepository.save(position);

        // Save Transaction Log
        saveTransaction(portfolio, ticker, "BUY", sharesToBuy, price, cost, reason);
        log.info("Executed BUY {} {} shares at {} VND. Reason: {}", ticker, sharesToBuy, price, reason);
    }

    private void executeSell(Portfolio portfolio, String ticker, BigDecimal price, String reason) {
        Optional<PortfolioPosition> existingPosOpt = positionRepository.findByPortfolioIdAndTicker(portfolio.getId(),
                ticker);
        if (existingPosOpt.isEmpty() || existingPosOpt.get().getQuantity() <= 0) {
            log.info("Skip SELL {}: No open position to sell.", ticker);
            return;
        }

        PortfolioPosition position = existingPosOpt.get();
        int sharesToSell = position.getQuantity(); // Sell All for simplicity in Phase 1
        BigDecimal revenue = price.multiply(new BigDecimal(sharesToSell));

        // Update Cash
        portfolio.setCashBalance(portfolio.getCashBalance().add(revenue));
        portfolioRepository.save(portfolio);

        // Remove Position
        positionRepository.delete(position);

        // Save Transaction Log
        saveTransaction(portfolio, ticker, "SELL", sharesToSell, price, revenue, reason);
        log.info("Executed SELL {} {} shares at {} VND. Reason: {}", ticker, sharesToSell, price, reason);
    }

    private void saveTransaction(Portfolio portfolio, String ticker, String type, int quantity, BigDecimal price,
            BigDecimal totalValue, String reason) {
        PortfolioTransaction tx = new PortfolioTransaction();
        tx.setPortfolio(portfolio);
        tx.setTicker(ticker);
        tx.setType(type);
        tx.setQuantity(quantity);
        tx.setPrice(price);
        tx.setTotalValue(totalValue);
        tx.setReason(reason);
        transactionRepository.save(tx);
    }

    public BigDecimal calculateTotalEquity(Portfolio portfolio) {
        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());
        BigDecimal stockValue = BigDecimal.ZERO;

        for (PortfolioPosition pos : positions) {
            Optional<StockQuote> quoteOpt = stockQuoteService.getLatestQuote(pos.getTicker());
            BigDecimal currentPrice = quoteOpt.isPresent() && quoteOpt.get().getMatchPrice() != null
                    ? BigDecimal.valueOf(quoteOpt.get().getMatchPrice()).multiply(new BigDecimal("1000"))
                    : pos.getAveragePrice();

            stockValue = stockValue.add(currentPrice.multiply(new BigDecimal(pos.getQuantity())));
        }

        return portfolio.getCashBalance().add(stockValue);
    }

    public List<java.util.Map<String, Object>> getEnrichedPositions(Portfolio portfolio) {
        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        for (PortfolioPosition pos : positions) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", pos.getId());
            map.put("ticker", pos.getTicker());
            map.put("quantity", pos.getQuantity());
            map.put("averagePrice", pos.getAveragePrice());

            Optional<StockQuote> quoteOpt = stockQuoteService.getLatestQuote(pos.getTicker());
            BigDecimal currentPrice = quoteOpt.isPresent() && quoteOpt.get().getMatchPrice() != null
                    ? BigDecimal.valueOf(quoteOpt.get().getMatchPrice()).multiply(new BigDecimal("1000"))
                    : pos.getAveragePrice();

            map.put("currentPrice", currentPrice);

            BigDecimal currentTotal = currentPrice.multiply(new BigDecimal(pos.getQuantity()));
            BigDecimal costTotal = pos.getAveragePrice().multiply(new BigDecimal(pos.getQuantity()));
            BigDecimal pnlValue = currentTotal.subtract(costTotal);

            BigDecimal pnlPercent = BigDecimal.ZERO;
            if (costTotal.compareTo(BigDecimal.ZERO) > 0) {
                pnlPercent = pnlValue.divide(costTotal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }

            map.put("pnlValue", pnlValue);
            map.put("pnlPercent", pnlPercent);

            result.add(map);
        }
        return result;
    }

    @Scheduled(cron = "0 15 15 * * MON-FRI", zone = "Asia/Ho_Chi_Minh") // 15:15 Every weekday (After market close)
    @Transactional
    public void takeDailySnapshot() {
        log.info("Taking daily portfolio snapshot...");
        Portfolio portfolio = getDefaultPortfolio();

        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());
        BigDecimal stockValue = BigDecimal.ZERO;

        for (PortfolioPosition pos : positions) {
            Optional<StockQuote> quoteOpt = stockQuoteService.getLatestQuote(pos.getTicker());
            BigDecimal currentPrice = quoteOpt.isPresent() && quoteOpt.get().getMatchPrice() != null
                    ? BigDecimal.valueOf(quoteOpt.get().getMatchPrice()).multiply(new BigDecimal("1000"))
                    : pos.getAveragePrice();

            stockValue = stockValue.add(currentPrice.multiply(new BigDecimal(pos.getQuantity())));
        }

        BigDecimal totalEquity = portfolio.getCashBalance().add(stockValue);

        PortfolioSnapshot snapshot = snapshotRepository
                .findByPortfolioIdAndSnapshotDate(portfolio.getId(), LocalDate.now())
                .orElse(new PortfolioSnapshot());

        snapshot.setPortfolio(portfolio);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setCashBalance(portfolio.getCashBalance());
        snapshot.setStockValue(stockValue);
        snapshot.setTotalEquity(totalEquity);

        snapshotRepository.save(snapshot);
        log.info("Saved Portfolio Snapshot: Equity {}, Cash {}", totalEquity, portfolio.getCashBalance());
    }
}
