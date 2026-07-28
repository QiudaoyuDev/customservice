package com.hardwareai.support.identity;

import com.hardwareai.support.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues short-lived HMAC-signed access tokens; no credential material is ever logged.
 */
@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(AppProperties p) {
        this.key = Keys.hmacShaKeyFor(p.security().jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    String issue(UserAccount u) {
        return Jwts.builder()
                .subject(u.id().toString())
                .claim("tenantId", u.tenantId().toString())
                .claim("role", u.role().name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(28800)))
                .signWith(key)
                .compact();
    }

    io.jsonwebtoken.Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
