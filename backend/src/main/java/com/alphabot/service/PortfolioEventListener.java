package com.alphabot.service;

import com.alphabot.dto.TradeExecutedEvent;
import com.alphabot.entity.Portfolio;
import com.alphabot.entity.PortfolioSnapshot;
import com.alphabot.repository.PortfolioSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioEventListener {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioService portfolioService;

    @Async
    @EventListener
    @Transactional
    public void handleTradeExecuted(TradeExecutedEvent event) {
        Portfolio portfolio = event.getTransaction().getPortfolio();
        log.info("Async processing trade for {} - Ticker: {}", portfolio.getName(), event.getTransaction().getTicker());

        // Update PnL logic could go here, or just refresh the snapshot
        updateDailySnapshot(portfolio);
    }

    private void updateDailySnapshot(Portfolio portfolio) {
        BigDecimal totalEquity = portfolioService.calculateTotalEquity(portfolio);

        PortfolioSnapshot snapshot = snapshotRepository
                .findByPortfolioIdAndSnapshotDate(portfolio.getId(), LocalDate.now())
                .orElse(new PortfolioSnapshot());

        snapshot.setPortfolio(portfolio);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setCashBalance(portfolio.getCashBalance());
        snapshot.setTotalEquity(totalEquity);
        // We'd need to calculate stock value properly if needed, but for snapshot it's
        // fine
        snapshot.setStockValue(totalEquity.subtract(portfolio.getCashBalance()));

        snapshotRepository.save(snapshot);
        log.info("Updated snapshot for {} - New Equity: {}", portfolio.getName(), totalEquity);
    }
}
