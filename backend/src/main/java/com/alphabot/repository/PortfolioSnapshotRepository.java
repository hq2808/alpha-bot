package com.alphabot.repository;

import com.alphabot.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {
    List<PortfolioSnapshot> findByPortfolioIdOrderBySnapshotDateAsc(Long portfolioId);

    Optional<PortfolioSnapshot> findByPortfolioIdAndSnapshotDate(Long portfolioId, LocalDate snapshotDate);
}
