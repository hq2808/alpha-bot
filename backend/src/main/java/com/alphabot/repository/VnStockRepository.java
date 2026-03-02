package com.alphabot.repository;

import com.alphabot.entity.VnStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VnStockRepository extends JpaRepository<VnStock, String> {
    List<VnStock> findAllByOrderBySectorAscTickerAsc();
}
