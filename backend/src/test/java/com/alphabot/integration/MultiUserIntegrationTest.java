package com.alphabot.integration;

import com.alphabot.entity.Portfolio;
import com.alphabot.entity.PortfolioType;
import com.alphabot.entity.User;
import com.alphabot.repository.PortfolioRepository;
import com.alphabot.repository.UserRepository;
import com.alphabot.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MultiUserIntegrationTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Test
    void testPortfolioIsolationBetweenUsers() {
        // 1. Setup two users
        User userA = userRepository.save(User.builder()
                .provider("google")
                .providerId("user-a")
                .email("user-a@gmail.com")
                .build());

        User userB = userRepository.save(User.builder()
                .provider("google")
                .providerId("user-b")
                .email("user-b@gmail.com")
                .build());

        // 2. Create portfolios
        Portfolio portA = portfolioService.getOrCreatePortfolio(userA, PortfolioType.AUTO);
        Portfolio portB = portfolioService.getOrCreatePortfolio(userB, PortfolioType.AUTO);

        assertNotEquals(portA.getId(), portB.getId());
        assertEquals(userA.getId(), portA.getUser().getId());
        assertEquals(userB.getId(), portB.getUser().getId());

        // 3. Perform action for User A (e.g., change balance)
        BigDecimal newBalanceA = new BigDecimal("50000000");
        portA.setCashBalance(newBalanceA);
        portfolioRepository.save(portA);

        // 4. Verify User B is unaffected
        Portfolio portBAfter = portfolioRepository.findById(portB.getId()).orElseThrow();
        assertEquals(new BigDecimal("100000000.00"), portBAfter.getCashBalance());

        // 5. Verify independent retrieval
        Portfolio portAFetched = portfolioService.getOrCreatePortfolio(userA, PortfolioType.AUTO);
        assertEquals(newBalanceA, portAFetched.getCashBalance());
    }
}
