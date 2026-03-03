package com.alphabot.controller;

import com.alphabot.entity.VnStock;
import com.alphabot.entity.Watchlist;
import com.alphabot.repository.VnStockRepository;
import com.alphabot.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:4200", "http://frontend:80" })
@io.swagger.v3.oas.annotations.tags.Tag(name = "Watchlist", description = "Endpoints for managing user stock watchlists and stock catalog")
public class WatchlistController {

    private final WatchlistRepository watchlistRepository;
    private final VnStockRepository vnStockRepository;

    /** GET /api/stocks — full VN stock catalog */
    @GetMapping("/api/stocks")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get stock catalog", description = "Returns a full list of all supported Vietnam stock tickers.")
    public List<VnStock> getStocks() {
        return vnStockRepository.findAllByOrderBySectorAscTickerAsc();
    }

    @GetMapping("/api/watchlist")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get user watchlist", description = "Returns the list of stocks currently in the user's watchlist.")
    public List<Watchlist> getWatchlist() {
        return watchlistRepository.findAll();
    }

    @PostMapping("/api/watchlist/{ticker}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Add ticker to watchlist", description = "Adds a stock ticker to the user's personal watchlist.")
    public ResponseEntity<Watchlist> addTicker(@PathVariable String ticker) {
        String reqTicker = ticker.toUpperCase();
        if (watchlistRepository.existsByTicker(reqTicker)) {
            return ResponseEntity.badRequest().build();
        }

        Watchlist item = Watchlist.builder()
                .ticker(reqTicker)
                .createdAt(Instant.now())
                .build();
        return ResponseEntity.ok(watchlistRepository.save(item));
    }

    @DeleteMapping("/api/watchlist/{ticker}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Remove ticker from watchlist", description = "Deletes a stock ticker from the user's personal watchlist.")
    @Transactional
    public ResponseEntity<Void> removeTicker(@PathVariable String ticker) {
        String reqTicker = ticker.toUpperCase();
        if (!watchlistRepository.existsByTicker(reqTicker)) {
            return ResponseEntity.notFound().build();
        }
        watchlistRepository.deleteByTicker(reqTicker);
        return ResponseEntity.ok().build();
    }
}
