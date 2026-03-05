package com.alphabot.controller;

import com.alphabot.dto.EnrichedPositionResponse;
import com.alphabot.dto.PortfolioSummaryResponse;
import com.alphabot.dto.TradeOrderRequest;
import com.alphabot.entity.*;
import com.alphabot.repository.*;
import com.alphabot.security.AuthenticatedUser;
import com.alphabot.service.AiTradingEngine;
import com.alphabot.service.ManualTradingService;
import com.alphabot.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Portfolio Management", description = "Endpoints for managing AI and Manual portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final ManualTradingService manualTradingService;
    private final AiTradingEngine aiTradingEngine;
    private final PortfolioTransactionRepository transactionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;

    @GetMapping("/summary")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get AI Portfolio summary", description = "Returns overview of the AI-managed portfolio including total equity and cash balance.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.AUTO);
        return ResponseEntity.ok(portfolioService.getSummaryData(portfolio));
    }

    @GetMapping("/manual/summary")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get Manual Portfolio summary", description = "Returns overview of the user's manual trading portfolio.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved manual summary")
    public ResponseEntity<PortfolioSummaryResponse> getManualSummary(@AuthenticatedUser User user) {
        return ResponseEntity.ok(manualTradingService.getSummary(user.getId()));
    }

    @GetMapping("/manual/positions")
    public ResponseEntity<List<EnrichedPositionResponse>> getManualPositions(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.MANUAL);
        return ResponseEntity.ok(portfolioService.getEnrichedPositions(portfolio));
    }

    @GetMapping("/manual/transactions")
    public ResponseEntity<List<PortfolioTransaction>> getManualTransactions(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.MANUAL);
        return ResponseEntity.ok(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId()));
    }

    @PostMapping("/manual/trade")
    @io.swagger.v3.oas.annotations.Operation(summary = "Execute manual trade", description = "Executes a buy or sell order for the manual portfolio.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trade executed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid trade request (insufficient funds/shares)", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.alphabot.dto.ErrorResponse.class)))
    })
    public ResponseEntity<Object> executeManualTrade(@AuthenticatedUser User user,
            @RequestBody TradeOrderRequest order) {
        manualTradingService.executeTrade(user.getId(), order);
        return ResponseEntity.ok("Trade executed successfully");
    }

    @PostMapping("/manual/reset")
    public ResponseEntity<Object> resetManualPortfolio(@AuthenticatedUser User user) {
        manualTradingService.resetPortfolio(user.getId());
        return ResponseEntity.ok("Manual portfolio reset successfully");
    }

    @GetMapping("/positions")
    public ResponseEntity<List<EnrichedPositionResponse>> getPositions(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.AUTO);
        return ResponseEntity.ok(portfolioService.getEnrichedPositions(portfolio));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<PortfolioTransaction>> getTransactions(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.AUTO);
        return ResponseEntity.ok(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId()));
    }

    @GetMapping("/chart")
    public ResponseEntity<List<PortfolioSnapshot>> getChartData(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.AUTO);
        return ResponseEntity.ok(snapshotRepository.findByPortfolioIdOrderBySnapshotDateAsc(portfolio.getId()));
    }

    @PostMapping("/snapshot")
    public ResponseEntity<String> forceSnapshot(@AuthenticatedUser User user) {
        Portfolio portfolio = portfolioService.getOrCreatePortfolio(user, PortfolioType.AUTO);
        portfolioService.takeSnapshot(portfolio);
        return ResponseEntity.ok("Snapshot taken successfully");
    }

    @PostMapping("/trade/dry-run")
    public ResponseEntity<List<TradeOrderRequest>> runDryRunTrading() {
        List<TradeOrderRequest> plannedOrders = aiTradingEngine.executeDryRun();
        return ResponseEntity.ok(plannedOrders);
    }
}
