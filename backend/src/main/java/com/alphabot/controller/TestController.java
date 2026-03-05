package com.alphabot.controller;

import com.alphabot.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/api/health/token")
    public String getTestToken() {
        return jwtTokenProvider.generateToken("test@example.com", "test-sub-123");
    }
}
