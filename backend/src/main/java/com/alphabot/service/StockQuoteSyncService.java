package com.alphabot.service;

import com.alphabot.entity.StockQuote;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockQuoteSyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MarketSessionService marketSessionService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Using a predefined list of popular VN30 and large cap stocks for the board
    private final String TRACK_LIST = "SSI,VND,HCM,VCI,HPG,HSG,NKG,VHM,VIC,VRE,NVL,DIG,DXG,TCB,MBB,VPB,STB,CTG,VCB,BID,FPT,MWG,PNJ,GAS,PLX,POW,VNM,MSN,SAB,VJC,HVN,GVR,DGC,DPM,DCM,KBC,IDC,VGC";

    // Redis key
    private static final String REDIS_KEY_QUOTES = "Market:Quotes";

    // Run on Startup to ensure Redis has data even if market is closed
    @EventListener(ApplicationReadyEvent.class)
    public void initQuotes() {
        log.info("Startup: Initializing stock quotes cache...");
        performSync();
    }

    // Run every 5 seconds
    @Scheduled(fixedDelay = 5000)
    public void syncStockQuotes() {
        if (!marketSessionService.isMarketOpen()) {
            log.trace("Market Closed, skip sync/push");
            return;
        }
        performSync();
    }

    private void performSync() {
        try {
            List<StockQuote> quotesToSave = new ArrayList<>();
            Instant now = Instant.now();

            // CafeF has different centers: 1 = HOSE, 2 = HNX. We will fetch both to cover
            // our track list.
            String[] urls = {
                    "https://banggia.cafef.vn/stockhandler.ashx?center=1",
                    "https://banggia.cafef.vn/stockhandler.ashx?center=2"
            };

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.set("Referer", "https://banggia.cafef.vn/");
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

            for (String urlStr : urls) {
                try {
                    URI uri = URI.create(urlStr);
                    ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
                    String responseStr = response.getBody();

                    if (responseStr != null) {
                        JsonNode dataArray = objectMapper.readTree(responseStr);

                        if (dataArray.isArray()) {
                            for (JsonNode d : dataArray) {
                                String ticker = d.path("a").asText();
                                // Only save if it's in our tracking list
                                if (TRACK_LIST.contains(ticker)) {
                                    StockQuote quote = StockQuote.builder()
                                            .ticker(ticker)
                                            .basicPrice(d.path("b").asDouble(0))
                                            .ceilingPrice(d.path("c").asDouble(0))
                                            .floorPrice(d.path("d").asDouble(0))
                                            .matchPrice(d.path("l").asDouble(0))
                                            .matchQtty(d.path("m").asDouble(0))
                                            // Bid 1
                                            .buyPrice1(d.path("i").asDouble(0))
                                            .buyQtty1(d.path("j").asDouble(0))
                                            // Bid 2
                                            .buyPrice2(d.path("g").asDouble(0))
                                            .buyQtty2(d.path("h").asDouble(0))
                                            // Bid 3
                                            .buyPrice3(d.path("e").asDouble(0))
                                            .buyQtty3(d.path("f").asDouble(0))
                                            // Ask 1
                                            .sellPrice1(d.path("o").asDouble(0))
                                            .sellQtty1(d.path("p").asDouble(0))
                                            // Ask 2
                                            .sellPrice2(d.path("q").asDouble(0))
                                            .sellQtty2(d.path("r").asDouble(0))
                                            // Ask 3
                                            .sellPrice3(d.path("s").asDouble(0))
                                            .sellQtty3(d.path("t").asDouble(0))
                                            // Total match
                                            .totalMatchQtty(d.path("n").asDouble(0))
                                            .updatedAt(now)
                                            .build();
                                    quotesToSave.add(quote);
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.error("Failed to fetch CafeF center: {}", urlStr, ex);
                }
            }

            if (!quotesToSave.isEmpty()) {
                // Save to Redis Hash instead of postgres
                for (StockQuote q : quotesToSave) {
                    redisTemplate.opsForHash().put(REDIS_KEY_QUOTES, q.getTicker(), q);
                }
                // CLUSTER SYNC: Publish to Redis instead of direct WebSocket
                // This allows 1M users spread across multiple nodes to receive the update
                try {
                    String json = objectMapper.writeValueAsString(quotesToSave);
                    redisTemplate.convertAndSend("market-ticks", json);
                } catch (Exception e) {
                    log.error("Failed to serialize market ticks for Redis Pub/Sub", e);
                }
            }

        } catch (Exception e) {
            log.error("Failed to sync stock quotes from CafeF: {}", e.getMessage());
        }
    }

    public Optional<StockQuote> getLatestQuote(String ticker) {
        Object data = redisTemplate.opsForHash().get(REDIS_KEY_QUOTES, ticker);
        if (data != null) {
            try {
                if (data instanceof StockQuote) {
                    return Optional.of((StockQuote) data);
                }
                // Fallback for LinkedHashMap conversion
                StockQuote quote = objectMapper.convertValue(data, StockQuote.class);
                return Optional.of(quote);
            } catch (Exception e) {
                log.error("Error parsing StockQuote from Redis for {}: {}", ticker, e.getMessage());
            }
        }
        return Optional.empty();
    }
}
