package com.alphabot.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${alpha-bot.jwt.secret}")
    private String jwtSecret;

    @Value("${alpha-bot.jwt.expiration-ms:86400000}") // 1 day default
    private long jwtExpirationMs;

    public String generateToken(String email, String providerId) {
        try {
            MACSigner signer = new MACSigner(jwtSecret.getBytes());

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(providerId)
                    .claim("email", email)
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet);

            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to generate JWT token", e);
            throw new RuntimeException("Could not generate JWT token", e);
        }
    }
}
