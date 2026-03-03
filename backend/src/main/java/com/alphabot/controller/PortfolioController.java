package com.alphabot.controller;

import com.alphabot.entity.*;
import com.alphabot.repository.*;
import com.alphabot.service.AiTradingEngine;
import com.alphabot.service.ManualTradingService;
import com.alphabot.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ManualTradingService manualTradingService;
    private final AiTradingEngine aiTradingEngine;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Portfolio portfolio = portfolioService.getDefaultPortfolio();
        return ResponseEntity.ok(portfolioService.getSummaryData(portfolio));
    }

    @GetMapping("/manual/summary")
    public ResponseEntity<Map<String, Object>> getManualSummary() {
        return ResponseEntity.ok(manualTradingService.getSummary());
    }

    @GetMapping("/manual/positions")
    public ResponseEntity<List<Map<String, Object>>> getManualPositions() {
        Portfolio portfolio = portfolioRepository.findByUserIdAndType(1L, PortfolioType.MANUAL)
                .orElseThrow(() -> new RuntimeException("Manual portfolio not found"));
        return ResponseEntity.ok(portfolioService.getEnrichedPositions(portfolio));
    }

    @GetMapping("/manual/transactions")
    public ResponseEntity<List<PortfolioTransaction>> getManualTransactions() {
        Portfolio portfolio = portfolioRepository.findByUserIdAndType(1L, PortfolioType.MANUAL)
                .orElseThrow(() -> new RuntimeException("Manual portfolio not found"));
        return ResponseEntity.ok(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId()));
    }

    @PostMapping("/manual/trade")
    public ResponseEntity<Object> executeManualTrade(@RequestBody com.alphabot.dto.TradeOrderRequest order) {
        try {
            manualTradingService.executeTrade(order);
            return ResponseEntity.ok("Trade executed successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/manual/reset")
    public ResponseEntity<Object> resetManualPortfolio() {
        try {
            manualTradingService.resetPortfolio();
            return ResponseEntity.ok("Manual portfolio reset successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
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
