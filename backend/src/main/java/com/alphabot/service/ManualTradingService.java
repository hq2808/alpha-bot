package com.alphabot.service;

import com.alphabot.dto.TradeExecutedEvent;
import com.alphabot.dto.TradeOrderRequest;
import com.alphabot.entity.*;
import com.alphabot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManualTradingService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioPositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final StockQuoteSyncService stockQuoteService;
    private final PortfolioService portfolioService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${manual.initial-capital:100000000}")
    private BigDecimal initialCapital;

    private static final Long DEFAULT_USER_ID = 1L;

    @Transactional
    @CacheEvict(value = "portfolioSummary", key = "#userId")
    public void executeTrade(Long userId, TradeOrderRequest order) {
        Portfolio portfolio = portfolioRepository.findWithLockByUserIdAndType(userId, PortfolioType.MANUAL)
                .orElseThrow(() -> new RuntimeException("Manual portfolio not found for user: " + userId));

        String ticker = order.getTicker().toUpperCase();
        Optional<StockQuote> latestQuoteOpt = stockQuoteService.getLatestQuote(ticker);

        if (latestQuoteOpt.isEmpty() || latestQuoteOpt.get().getMatchPrice() == null
                || latestQuoteOpt.get().getMatchPrice() == 0.0) {
            throw new RuntimeException("Cannot execute trade: No valid market price for " + ticker);
        }

        BigDecimal price = BigDecimal.valueOf(latestQuoteOpt.get().getMatchPrice());
        int quantity = order.getQuantity();

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if ("BUY".equalsIgnoreCase(order.getAction())) {
            executeBuy(portfolio, ticker, price, quantity, order.getReason());
        } else if ("SELL".equalsIgnoreCase(order.getAction())) {
            executeSell(portfolio, ticker, price, quantity, order.getReason());
        }
    }

    private void executeBuy(Portfolio portfolio, String ticker, BigDecimal price, int quantity, String reason) {
        BigDecimal cost = price.multiply(new BigDecimal(quantity));

        if (portfolio.getCashBalance().compareTo(cost) < 0) {
            throw new RuntimeException("Insufficient cash balance");
        }

        // Update Portfolio
        portfolio.setCashBalance(portfolio.getCashBalance().subtract(cost));
        portfolioRepository.save(portfolio);

        // Update Position
        PortfolioPosition position = positionRepository.findByPortfolioIdAndTicker(portfolio.getId(), ticker)
                .orElse(new PortfolioPosition());

        if (position.getId() == null) {
            position.setPortfolio(portfolio);
            position.setTicker(ticker);
            position.setQuantity(quantity);
            position.setAveragePrice(price);
        } else {
            BigDecimal oldCost = position.getAveragePrice().multiply(new BigDecimal(position.getQuantity()));
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal newAveragePrice = oldCost.add(cost).divide(new BigDecimal(newQuantity), 4, RoundingMode.HALF_UP);
            position.setQuantity(newQuantity);
            position.setAveragePrice(newAveragePrice);
        }
        positionRepository.save(position);

        saveTransaction(portfolio, ticker, "BUY", quantity, price, cost, reason, price, null, null);
    }

    private void executeSell(Portfolio portfolio, String ticker, BigDecimal price, int quantity, String reason) {
        PortfolioPosition position = positionRepository.findByPortfolioIdAndTicker(portfolio.getId(), ticker)
                .orElseThrow(() -> new RuntimeException("No position found for ticker: " + ticker));

        if (position.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient quantity to sell");
        }

        BigDecimal revenue = price.multiply(new BigDecimal(quantity));
        BigDecimal costValue = position.getAveragePrice().multiply(new BigDecimal(quantity));
        BigDecimal pnlValue = revenue.subtract(costValue);
        BigDecimal pnlPercent = BigDecimal.ZERO;

        if (costValue.compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = pnlValue.divide(costValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        BigDecimal costPrice = position.getAveragePrice();

        // Update Portfolio
        portfolio.setCashBalance(portfolio.getCashBalance().add(revenue));
        portfolioRepository.save(portfolio);

        // Update Position
        if (position.getQuantity() == quantity) {
            positionRepository.delete(position);
        } else {
            position.setQuantity(position.getQuantity() - quantity);
            positionRepository.save(position);
        }

        saveTransaction(portfolio, ticker, "SELL", quantity, price, revenue, reason, costPrice, pnlValue, pnlPercent);
    }

    @Transactional
    @CacheEvict(value = "portfolioSummary", key = "#userId")
    public void resetPortfolio(Long userId) {
        Portfolio portfolio = portfolioRepository.findWithLockByUserIdAndType(userId, PortfolioType.MANUAL)
                .orElseThrow(() -> new RuntimeException("Manual portfolio not found"));

        // Delete all positions
        List<PortfolioPosition> positions = positionRepository.findByPortfolioId(portfolio.getId());
        positionRepository.deleteAll(positions);

        // Delete all transactions
        List<PortfolioTransaction> transactions = transactionRepository
                .findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());
        transactionRepository.deleteAll(transactions);

        // Reset balance
        portfolio.setCashBalance(initialCapital);
        portfolio.setInitialCapital(initialCapital);
        portfolioRepository.save(portfolio);

        log.info("Reset manual portfolio for user {}", userId);
    }

    @Cacheable(value = "portfolioSummary", key = "#userId")
    public Map<String, Object> getSummary(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserIdAndType(userId, PortfolioType.MANUAL)
                .orElseThrow(() -> new RuntimeException("Portfolio not found"));

        BigDecimal totalEquity = portfolioService.calculateTotalEquity(portfolio);
        BigDecimal pnlValue = totalEquity.subtract(portfolio.getInitialCapital());
        BigDecimal pnlPercent = BigDecimal.ZERO;
        if (portfolio.getInitialCapital().compareTo(BigDecimal.ZERO) > 0) {
            pnlPercent = pnlValue.divide(portfolio.getInitialCapital(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return Map.of(
                "name", portfolio.getName(),
                "initialCapital", portfolio.getInitialCapital(),
                "cashBalance", portfolio.getCashBalance(),
                "totalEquity", totalEquity,
                "pnlValue", pnlValue,
                "pnlPercent", pnlPercent);
    }

    private void saveTransaction(Portfolio portfolio, String ticker, String type, int quantity, BigDecimal price,
            BigDecimal totalValue, String reason, BigDecimal costPrice, BigDecimal pnlValue, BigDecimal pnlPercent) {
        PortfolioTransaction tx = new PortfolioTransaction();
        tx.setPortfolio(portfolio);
        tx.setTicker(ticker);
        tx.setType(type);
        tx.setQuantity(quantity);
        tx.setPrice(price);
        tx.setTotalValue(totalValue);
        tx.setReason(reason);
        tx.setCostPrice(costPrice);
        tx.setPnlValue(pnlValue);
        tx.setPnlPercent(pnlPercent);
        transactionRepository.save(tx);

        // Publish event for async processing
        eventPublisher.publishEvent(new TradeExecutedEvent(this, tx));
    }

    @Transactional
    public void executeTrade(TradeOrderRequest order) {
        executeTrade(DEFAULT_USER_ID, order);
    }

    @Transactional
    public void resetPortfolio() {
        resetPortfolio(DEFAULT_USER_ID);
    }

    public Map<String, Object> getSummary() {
        return getSummary(DEFAULT_USER_ID);
    }
}
