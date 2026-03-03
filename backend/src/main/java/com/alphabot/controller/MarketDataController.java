package com.alphabot.controller;

import com.alphabot.entity.MarketData;
import com.alphabot.repository.MarketDataRepository;
import com.alphabot.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final MarketDataRepository marketDataRepository;
    private final MarketDataService marketDataService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/{ticker}")
    public ResponseEntity<List<MarketData>> getHistoricalData(@PathVariable String ticker) {
        return ResponseEntity.ok(getHistoricalDataList(ticker));
    }

    /**
     * Endpoint to fetch cached Level-2 real-time data from Redis
     */
    @GetMapping("/vndirect/quotes")
    public ResponseEntity<Map<String, Object>> getVndirectQuotes() {
        try {
            // Get all quotes from Redis Hash
            Map<Object, Object> entries = redisTemplate.opsForHash().entries("Market:Quotes");
            return ResponseEntity.ok(Map.of("data", entries.values()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch cached data from Redis"));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, List<MarketData>>> getHistoricalDataBatch(@RequestBody List<String> tickers) {
        Map<String, List<MarketData>> result = new HashMap<>();
        for (String ticker : tickers) {
            result.put(ticker, getHistoricalDataList(ticker));
        }
        return ResponseEntity.ok(result);
    }

    private List<MarketData> getHistoricalDataList(String ticker) {
        // Lấy dữ liệu từ database, xếp theo ngày cũ -> mới
        List<MarketData> data = marketDataRepository.findByTickerOrderByDateDesc(ticker);

        // Nếu không có dữ liệu, thử fetch trực tiếp từ Yahoo Finance
        if (data.isEmpty()) {
            marketDataService.fetchAndSaveRecentData(ticker);
            data = marketDataRepository.findByTickerOrderByDateDesc(ticker);
        }

        // Đảo ngược lại để Frontend vẽ chart từ Trái sang Phải (Cũ -> Mới)
        return data.stream()
                .sorted((d1, d2) -> d1.getDate().compareTo(d2.getDate()))
                .toList();
    }
}
