package com.alphabot.security;

import com.alphabot.entity.PortfolioType;
import com.alphabot.entity.User;
import com.alphabot.service.PortfolioService;
import com.alphabot.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final PortfolioService portfolioService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        String email = null;
        String providerId = null;
        String provider = "google"; // Default for now

        if (principal instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
            providerId = oidcUser.getSubject();
        } else if (principal instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
            providerId = oauth2User.getAttribute("sub");
            if (providerId == null)
                providerId = oauth2User.getName();
        }

        if (providerId != null) {
            log.info("Successfully logged in via OAuth2: {}. Syncing with local DB...", email);
            User user = userService.getOrCreateUser(provider, providerId, email != null ? email : "no-email@oauth.com");

            // Pre-create portfolios
            portfolioService.getOrCreatePortfolio(user, PortfolioType.AUTO);
            portfolioService.getOrCreatePortfolio(user, PortfolioType.MANUAL);

            log.info("User {} synced and portfolios verified.", email);

            // Generate JWT and pass it to frontend via URL query param
            String token = jwtTokenProvider.generateToken(email, providerId);
            response.sendRedirect("http://localhost:4200/dashboard?token=" + token);
            return;
        }

        // Fallback without token
        response.sendRedirect("http://localhost:4200/dashboard");
    }
}
