package com.alphabot.dto;

import com.alphabot.entity.PortfolioTransaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TradeExecutedEvent extends ApplicationEvent {
    private final PortfolioTransaction transaction;

    public TradeExecutedEvent(Object source, PortfolioTransaction transaction) {
        super(source);
        this.transaction = transaction;
    }
}
