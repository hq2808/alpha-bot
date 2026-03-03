package com.alphabot.service;

import com.alphabot.entity.MarketData;
import com.alphabot.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // Dữ liệu mẫu: Cổ phiếu công nghệ Mỹ hoặc các mã VN (nếu dùng nguồn dữ liệu VN)
    private static final List<String> WATCH_LIST = List.of("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA");

    /**
     * Đồng bộ dữ liệu giá hàng ngày từ Yahoo Finance lúc 01:00 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduleDailySync() {
        log.info("Bắt đầu tiến trình đồng bộ giá đóng cửa cho Watchlist...");
        for (String ticker : WATCH_LIST) {
            fetchAndSaveRecentData(ticker);
        }
        log.info("Hoàn tất tiến trình đồng bộ giá.");
    }

    public void fetchAndSaveRecentData(String ticker) {
        try {
            log.info("Đang lấy dữ liệu giá cho mã {}...", ticker);
            // Append .VN for 3-letter Vietnamese stocks on Yahoo Finance
            String queryTicker = ticker;
            if (ticker.length() == 3 && !ticker.contains(".")) {
                queryTicker = ticker + ".VN";
            }

            // Lấy dữ liệu 5 ngày gần nhất
            String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?range=5d&interval=1d",
                    queryTicker);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                parseAndSaveYahooFinanceResponse(ticker, resp.getBody());
            } else {
                log.warn("Lấy dữ liệu giá không thành công cho {}", ticker);
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy dữ liệu giá cho {}: {}", ticker, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAndSaveYahooFinanceResponse(String ticker, Map<String, Object> response) {
        if (response == null || !response.containsKey("chart"))
            return;

        Map<String, Object> chart = (Map<String, Object>) response.get("chart");
        List<Object> resultList = (List<Object>) chart.get("result");
        if (resultList == null || resultList.isEmpty())
            return;

        Map<String, Object> result = (Map<String, Object>) resultList.get(0);
        List<Integer> timestamps = (List<Integer>) result.get("timestamp");

        Map<String, Object> indicators = (Map<String, Object>) result.get("indicators");
        List<Object> quoteList = (List<Object>) indicators.get("quote");
        if (quoteList == null || quoteList.isEmpty())
            return;

        Map<String, Object> quote = (Map<String, Object>) quoteList.get(0);

        List<Number> openList = (List<Number>) quote.get("open");
        List<Number> highList = (List<Number>) quote.get("high");
        List<Number> lowList = (List<Number>) quote.get("low");
        List<Number> closeList = (List<Number>) quote.get("close");
        List<Number> volumeList = (List<Number>) quote.get("volume");

        if (timestamps == null)
            return;

        List<MarketData> toSave = new java.util.ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            if (closeList == null || i >= closeList.size() || closeList.get(i) == null)
                continue; // Bỏ qua dữ liệu null

            Instant date = Instant.ofEpochSecond(timestamps.get(i).longValue());

            Optional<MarketData> existing = marketDataRepository.findByTickerAndDate(ticker, date);
            if (existing.isEmpty()) {
                MarketData data = MarketData.builder()
                        .ticker(ticker)
                        .date(date)
                        .open(openList != null && i < openList.size() && openList.get(i) != null
                                ? openList.get(i).doubleValue() * 1000
                                : null)
                        .high(highList != null && i < highList.size() && highList.get(i) != null
                                ? highList.get(i).doubleValue() * 1000
                                : null)
                        .low(lowList != null && i < lowList.size() && lowList.get(i) != null
                                ? lowList.get(i).doubleValue() * 1000
                                : null)
                        .close(closeList.get(i).doubleValue() * 1000)
                        .volume(volumeList != null && i < volumeList.size() && volumeList.get(i) != null
                                ? volumeList.get(i).longValue()
                                : 0L)
                        .build();
                toSave.add(data);
            }
        }

        if (!toSave.isEmpty()) {
            marketDataRepository.saveAll(toSave);
        }
        log.info("Lưu thành công {} bản ghi mới cho mã {}.", toSave.size(), ticker);
    }
}
