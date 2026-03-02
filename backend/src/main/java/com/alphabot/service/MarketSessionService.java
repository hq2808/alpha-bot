package com.alphabot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
@Slf4j
public class MarketSessionService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Phiên sáng: 09:00 - 11:30
    private static final LocalTime MORNING_START = LocalTime.of(9, 0);
    private static final LocalTime MORNING_END = LocalTime.of(11, 30);

    // Phiên chiều: 13:00 - 15:00
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);

    public boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        DayOfWeek day = now.getDayOfWeek();

        // Cuối tuần nghỉ giao dịch
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();

        boolean isMorningSession = !time.isBefore(MORNING_START) && !time.isAfter(MORNING_END);
        boolean isAfternoonSession = !time.isBefore(AFTERNOON_START) && !time.isAfter(AFTERNOON_END);

        return isMorningSession || isAfternoonSession;
    }
}
