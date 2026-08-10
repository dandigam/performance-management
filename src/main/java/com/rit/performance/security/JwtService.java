package com.rit.performance.security;

import com.rit.performance.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:performance-management-system}")
    private String issuer;

    @Value("${jwt.audience:performance-management-ui}")
    private String audience;

    @Value("${jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    private final AppUserDetailsService userDetailsService;

    public JwtService(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    void validateConfiguration() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret must contain at least 32 bytes");
        }
        if (accessExpirationMs <= 0) {
            throw new IllegalStateException("jwt.access-expiration-ms must be positive");
        }
    }

    public AccessToken createAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(accessExpirationMs);
        String authority = userDetailsService.roleAuthority(user.getRole());
        String token = Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .claim("role", authority)
                .claim("tokenType", "access")
                .signWith(signingKey())
                .compact();
        return new AccessToken(token, expiresAt);
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!"access".equals(claims.get("tokenType", String.class))) {
            throw new IllegalArgumentException("Invalid token type");
        }
        return claims;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public record AccessToken(String value, Instant expiresAt) {
    }
}
