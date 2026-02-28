package com.alphabot.repository;

import com.alphabot.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    List<MarketData> findByTickerOrderByDateDesc(String ticker);

    List<MarketData> findByTickerAndDateBetweenOrderByDateAsc(String ticker, Instant startDate, Instant endDate);

    Optional<MarketData> findByTickerAndDate(String ticker, Instant date);
}
