package com.alphabot.security;

import com.alphabot.entity.User;
import com.alphabot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class) &&
                parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            String email = jwt.getClaim("email");
            String issuer = jwt.getClaim("iss");
            String provider = (issuer != null && issuer.contains("github")) ? "github" : "google";
            return userService.getOrCreateUser(provider, subject, email != null ? email : "no-email@oauth.com");
        }

        if (principal instanceof OidcUser oidcUser) {
            return userService.getOrCreateUser("google", oidcUser.getSubject(), oidcUser.getEmail());
        }

        if (principal instanceof OAuth2User oauth2User) {
            String subject = oauth2User.getAttribute("sub");
            if (subject == null)
                subject = oauth2User.getName();
            String email = oauth2User.getAttribute("email");
            return userService.getOrCreateUser("google", subject, email != null ? email : "no-email@oauth.com");
        }

        log.warn("Unknown principal type: {}", principal.getClass().getName());
        return null;
    }
}
