package com.alphabot.service;

import com.alphabot.dto.TradeOrderRequest;
import com.alphabot.entity.*;
import com.alphabot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualTradingServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private PortfolioPositionRepository positionRepository;
    @Mock
    private PortfolioTransactionRepository transactionRepository;
    @Mock
    private StockQuoteSyncService stockQuoteService;
    @Mock
    private PortfolioService portfolioService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ManualTradingService manualTradingService;

    private Portfolio manualPortfolio;
    private StockQuote stockQuote;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        manualPortfolio = new Portfolio();
        manualPortfolio.setId(1L);
        manualPortfolio.setUser(user);
        manualPortfolio.setType(PortfolioType.MANUAL);
        manualPortfolio.setCashBalance(new BigDecimal("100000000"));
        manualPortfolio.setInitialCapital(new BigDecimal("100000000"));

        stockQuote = new StockQuote();
        stockQuote.setTicker("FPT");
        stockQuote.setMatchPrice(100000.0);
    }

    @Test
    void executeTrade_BuySuccess() {
        TradeOrderRequest order = new TradeOrderRequest();
        order.setTicker("FPT");
        order.setAction("BUY");
        order.setQuantity(100);
        order.setReason("Test buy");

        when(portfolioRepository.findWithLockByUserIdAndType(1L, PortfolioType.MANUAL))
                .thenReturn(Optional.of(manualPortfolio));
        when(stockQuoteService.getLatestQuote("FPT")).thenReturn(Optional.of(stockQuote));
        when(positionRepository.findByPortfolioIdAndTicker(eq(1L), eq("FPT"))).thenReturn(Optional.empty());

        manualTradingService.executeTrade(1L, order);

        assertEquals(new BigDecimal("90000000.0"), manualPortfolio.getCashBalance());
        verify(portfolioRepository).save(manualPortfolio);
        verify(positionRepository).save(any(PortfolioPosition.class));
        verify(transactionRepository).save(any(PortfolioTransaction.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void executeTrade_BuyInsufficientCash() {
        TradeOrderRequest order = new TradeOrderRequest();
        order.setTicker("FPT");
        order.setAction("BUY");
        order.setQuantity(2000); // 2000 * 100,000 = 200,000,000 > 100,000,000
        order.setReason("Test buy");

        when(portfolioRepository.findWithLockByUserIdAndType(1L, PortfolioType.MANUAL))
                .thenReturn(Optional.of(manualPortfolio));
        when(stockQuoteService.getLatestQuote("FPT")).thenReturn(Optional.of(stockQuote));

        assertThrows(RuntimeException.class, () -> manualTradingService.executeTrade(1L, order));
    }

    @Test
    void executeTrade_SellSuccess() {
        PortfolioPosition position = new PortfolioPosition();
        position.setPortfolio(manualPortfolio);
        position.setTicker("FPT");
        position.setQuantity(100);
        position.setAveragePrice(new BigDecimal("90000"));

        TradeOrderRequest order = new TradeOrderRequest();
        order.setTicker("FPT");
        order.setAction("SELL");
        order.setQuantity(50);
        order.setReason("Test sell");

        when(portfolioRepository.findWithLockByUserIdAndType(1L, PortfolioType.MANUAL))
                .thenReturn(Optional.of(manualPortfolio));
        when(stockQuoteService.getLatestQuote("FPT")).thenReturn(Optional.of(stockQuote));
        when(positionRepository.findByPortfolioIdAndTicker(1L, "FPT")).thenReturn(Optional.of(position));

        manualTradingService.executeTrade(1L, order);

        assertEquals(new BigDecimal("105000000.0"), manualPortfolio.getCashBalance());
        assertEquals(50, position.getQuantity());
        verify(portfolioRepository).save(manualPortfolio);
        verify(positionRepository).save(position);

        // Verify PnL in transaction
        verify(transactionRepository).save(argThat(tx -> tx.getType().equals("SELL") &&
                tx.getCostPrice().compareTo(new BigDecimal("90000")) == 0 &&
                tx.getPnlValue().compareTo(new BigDecimal("500000.0")) == 0 && // (100k - 90k) * 50 = 500k
                tx.getPnlPercent().compareTo(new BigDecimal("11.1111")) == 0 // 10k / 90k * 100
        ));
    }

    @Test
    void executeTrade_SellWithoutPosition() {
        TradeOrderRequest order = new TradeOrderRequest();
        order.setTicker("FPT");
        order.setAction("SELL");
        order.setQuantity(50);

        when(portfolioRepository.findWithLockByUserIdAndType(1L, PortfolioType.MANUAL))
                .thenReturn(Optional.of(manualPortfolio));
        when(stockQuoteService.getLatestQuote("FPT")).thenReturn(Optional.of(stockQuote));
        when(positionRepository.findByPortfolioIdAndTicker(1L, "FPT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> manualTradingService.executeTrade(1L, order));
    }

    @Test
    void resetPortfolio_Success() {
        when(portfolioRepository.findWithLockByUserIdAndType(1L, PortfolioType.MANUAL))
                .thenReturn(Optional.of(manualPortfolio));

        manualTradingService.resetPortfolio(1L);

        verify(positionRepository).deleteAll(anyList());
        verify(transactionRepository).deleteAll(anyList());
        verify(portfolioRepository).save(manualPortfolio);
    }
}
