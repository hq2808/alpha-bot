package com.alphabot.controller;

import com.alphabot.entity.MarketData;
import com.alphabot.repository.MarketDataRepository;
import com.alphabot.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketDataController {

    private final MarketDataRepository marketDataRepository;
    private final MarketDataService marketDataService;

    @GetMapping("/{ticker}")
    public ResponseEntity<List<MarketData>> getHistoricalData(@PathVariable String ticker) {
        // Lấy dữ liệu từ database, xếp theo ngày cũ -> mới
        List<MarketData> data = marketDataRepository.findByTickerOrderByDateDesc(ticker);

        // Nếu không có dữ liệu, thử fetch trực tiếp từ Yahoo Finance
        if (data.isEmpty()) {
            marketDataService.fetchAndSaveRecentData(ticker);
            data = marketDataRepository.findByTickerOrderByDateDesc(ticker);
        }

        // Đảo ngược lại để Frontend vẽ chart từ Trái sang Phải (Cũ -> Mới)
        List<MarketData> chartData = data.stream()
                .sorted((d1, d2) -> d1.getDate().compareTo(d2.getDate()))
                .toList();

        return ResponseEntity.ok(chartData);
    }
}
