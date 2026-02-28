package com.alphabot.repository;

import com.alphabot.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    Optional<Watchlist> findByTicker(String ticker);

    boolean existsByTicker(String ticker);

    void deleteByTicker(String ticker);
}
