package com.alphabot.controller;

import com.alphabot.entity.Watchlist;
import com.alphabot.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistRepository watchlistRepository;

    @GetMapping
    public List<Watchlist> getWatchlist() {
        return watchlistRepository.findAll();
    }

    @PostMapping("/{ticker}")
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

    @DeleteMapping("/{ticker}")
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
