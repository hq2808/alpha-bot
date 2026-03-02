package com.alphabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Subscriber to Redis Pub/Sub topics.
 * When a message is received from Redis (from any node), this service
 * broadcasts it to all WebSocket clients connected to THIS instance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Callback method for Redis Message Listener
     */
    public void handleMarketTick(String message) {
        try {
            // We receive a JSON string (List of StockQuotes) from Redis Pub/Sub.
            // We broadcast it directly to the local WebSocket topic.
            // Using a raw string here is more efficient for high-throughput 1M CCU.
            messagingTemplate.convertAndSend("/topic/market-ticks", message);

            log.trace("Cluster Sync: Broadcasted market data to local users");
        } catch (Exception e) {
            log.error("Failed to handle market tick from Redis Pub/Sub: {}", e.getMessage());
        }
    }

    /**
     * Handle AI Recommendations via Redis Sync
     */
    public void handleAiRecommendation(String message) {
        try {
            // Simply forward the JSON to the topic
            messagingTemplate.convertAndSend("/topic/ai-recommendations", message);
            log.debug("Cluster Sync: Broadcasted AI Recomm to local users");
        } catch (Exception e) {
            log.error("Failed to handle AI recommendation from Redis: {}", e.getMessage());
        }
    }
}
