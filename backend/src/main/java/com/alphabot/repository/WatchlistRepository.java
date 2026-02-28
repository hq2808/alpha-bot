package com.alphabot.repository;

import com.alphabot.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    Optional<Watchlist> findByTicker(String ticker);

    boolean existsByTicker(String ticker);

    void deleteByTicker(String ticker);

    /**
     * Scale-ready method placeholder:
     * When Authentication is implemented, this should be scoped by User ID.
     * For now, with no user accounts, we select all unique tickers from the table.
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT w.ticker FROM Watchlist w")
    List<String> findAllTickersByUserId(Long userId);
}
