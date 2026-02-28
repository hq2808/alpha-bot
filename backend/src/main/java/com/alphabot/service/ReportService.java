package com.alphabot.service;

import com.alphabot.entity.NewsArticle;
import com.alphabot.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to generate and send scheduled reports (e.g., End of Day Summary).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final NewsArticleRepository newsArticleRepository;
    private final FinancialAssistant financialAssistant;
    private final AlertService alertService;

    /**
     * Runs every weekday (Mon-Fri) at 17:00 (5:00 PM) Vietnam Time.
     */
    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Asia/Ho_Chi_Minh")
    public void generateAndSendEodReport() {
        log.info("[ReportService] Triggering End of Day Telegram Report...");

        // 1. Calculate time boundary for today (from 00:00:00 today)
        ZonedDateTime nowVn = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime startOfDayVn = nowVn.truncatedTo(ChronoUnit.DAYS);
        Instant startOfDayInstant = startOfDayVn.toInstant();

        // 2. Fetch today's articles
        List<NewsArticle> todaysArticles = newsArticleRepository.findByCrawledAtAfter(startOfDayInstant);

        // 3. Guard: No news today
        if (todaysArticles == null || todaysArticles.isEmpty()) {
            log.info("[ReportService] No articles gathered today. Skipping EOD report.");
            // Send a tiny empty update so user knows the bot is alive but quiet
            alertService.sendEodReport("Không có sự kiện thị trường nào đáng chú ý được ghi nhận trong ngày hôm nay.");
            return;
        }

        log.info("[ReportService] Found {} articles today. Calling AI for summary...", todaysArticles.size());

        // 4. Prepare Context for AI
        String rawNewsText = todaysArticles.stream()
                .map(a -> String.format("- %s (Tickers: %s)", a.getTitle(), a.getMentionedTickers()))
                .collect(Collectors.joining("\n"));

        try {
            // 5. Generate AI Summary
            String reportMarkdown = financialAssistant.generateEodReport(rawNewsText);

            // 6. Send to Telegram
            alertService.sendEodReport(reportMarkdown);
            log.info("[ReportService] EOD Telegram Report generated and dispatched.");

        } catch (Exception e) {
            log.error("[ReportService] Error generating EOD Report: {}", e.getMessage());
        }
    }
}
