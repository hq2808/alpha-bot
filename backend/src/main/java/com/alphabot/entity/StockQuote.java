package com.alphabot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Entity
@Table(name = "stock_quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockQuote {

    @Id
    @Column(length = 20)
    private String ticker;

    @Column(name = "basic_price", columnDefinition = "numeric")
    private Double basicPrice;

    @Column(name = "ceiling_price", columnDefinition = "numeric")
    private Double ceilingPrice;

    @Column(name = "floor_price", columnDefinition = "numeric")
    private Double floorPrice;

    @Column(name = "match_price", columnDefinition = "numeric")
    private Double matchPrice;

    @Column(name = "match_qtty", columnDefinition = "numeric")
    private Double matchQtty;

    @Column(name = "buy_price_1", columnDefinition = "numeric")
    private Double buyPrice1;

    @Column(name = "buy_qtty_1", columnDefinition = "numeric")
    private Double buyQtty1;

    @Column(name = "buy_price_2", columnDefinition = "numeric")
    private Double buyPrice2;

    @Column(name = "buy_qtty_2", columnDefinition = "numeric")
    private Double buyQtty2;

    @Column(name = "buy_price_3", columnDefinition = "numeric")
    private Double buyPrice3;

    @Column(name = "buy_qtty_3", columnDefinition = "numeric")
    private Double buyQtty3;

    @Column(name = "sell_price_1", columnDefinition = "numeric")
    private Double sellPrice1;

    @Column(name = "sell_qtty_1", columnDefinition = "numeric")
    private Double sellQtty1;

    @Column(name = "sell_price_2", columnDefinition = "numeric")
    private Double sellPrice2;

    @Column(name = "sell_qtty_2", columnDefinition = "numeric")
    private Double sellQtty2;

    @Column(name = "sell_price_3", columnDefinition = "numeric")
    private Double sellPrice3;

    @Column(name = "sell_qtty_3", columnDefinition = "numeric")
    private Double sellQtty3;

    @Column(name = "total_match_qtty", columnDefinition = "numeric")
    private Double totalMatchQtty;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
