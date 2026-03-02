package com.alphabot.controller;

import com.alphabot.entity.*;
import com.alphabot.repository.*;
import com.alphabot.service.AiTradingEngine;
import com.alphabot.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AiTradingEngine aiTradingEngine;
    private final PortfolioPositionRepository positionRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Portfolio portfolio = portfolioService.getDefaultPortfolio();
        BigDecimal totalEquity = portfolioService.calculateTotalEquity(portfolio);

        // Caculate simple PnL vs Initial Capital
        BigDecimal pnlValue = totalEquity.subtract(portfolio.getInitialCapital());
        BigDecimal pnlPercent = pnlValue.divide(portfolio.getInitialCapital(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        Map<String, Object> summary = new HashMap<>();
        summary.put("name", portfolio.getName());
        summary.put("initialCapital", portfolio.getInitialCapital());
        summary.put("cashBalance", portfolio.getCashBalance());
        summary.put("totalEquity", totalEquity);
        summary.put("pnlValue", pnlValue);
        summary.put("pnlPercent", pnlPercent);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/positions")
    public ResponseEntity<List<Map<String, Object>>> getPositions() {
        Portfolio portfolio = portfolioService.getDefaultPortfolio();
        return ResponseEntity.ok(portfolioService.getEnrichedPositions(portfolio));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PortfolioTransaction>> getTransactions() {
        Portfolio portfolio = portfolioService.getDefaultPortfolio();
        return ResponseEntity.ok(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId()));
    }

    @GetMapping("/chart")
    public ResponseEntity<List<PortfolioSnapshot>> getChartData() {
        Portfolio portfolio = portfolioService.getDefaultPortfolio();
        return ResponseEntity.ok(snapshotRepository.findByPortfolioIdOrderBySnapshotDateAsc(portfolio.getId()));
    }

    @PostMapping("/snapshot")
    public ResponseEntity<String> forceSnapshot() {
        portfolioService.takeDailySnapshot();
        return ResponseEntity.ok("Snapshot taken successfully");
    }

    @PostMapping("/trade/dry-run")
    public ResponseEntity<List<com.alphabot.dto.TradeOrderRequest>> runDryRunTrading() {
        List<com.alphabot.dto.TradeOrderRequest> plannedOrders = aiTradingEngine.executeDryRun();
        return ResponseEntity.ok(plannedOrders);
    }
}
