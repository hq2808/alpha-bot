package com.alphabot.repository;

import com.alphabot.entity.StockQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockQuoteRepository extends JpaRepository<StockQuote, Long> {

    // Custom query to get latest quotes for all tickers
    @Query(value = "SELECT t1.* FROM stock_quotes t1 " +
            "INNER JOIN (SELECT ticker, MAX(updated_at) AS max_time " +
            "            FROM stock_quotes GROUP BY ticker) t2 " +
            "ON t1.ticker = t2.ticker AND t1.updated_at = t2.max_time", nativeQuery = true)
    List<StockQuote> findLatestQuotesForAllTickers();

    Optional<StockQuote> findTopByTickerOrderByUpdatedAtDesc(String ticker);
}
