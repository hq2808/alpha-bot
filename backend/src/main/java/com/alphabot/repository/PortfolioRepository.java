package com.alphabot.repository;

import com.alphabot.entity.Portfolio;
import com.alphabot.entity.PortfolioType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Optional<Portfolio> findByName(String name);

    Optional<Portfolio> findByUserIdAndType(Long userId, PortfolioType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolio p WHERE p.userId = :userId AND p.type = :type")
    Optional<Portfolio> findWithLockByUserIdAndType(Long userId, PortfolioType type);
}
