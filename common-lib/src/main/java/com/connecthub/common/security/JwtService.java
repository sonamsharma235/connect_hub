package com.connecthub.common.security;

import com.connecthub.common.dto.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtService(String secret, long expirationMillis) {
        this.secretKey = buildSecretKey(secret);
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(AuthUser authUser, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(authUser.email())
                .claim("name", authUser.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)));

        extraClaims.forEach(builder::claim);

        return builder
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public AuthUser parseUser(String token) {
        Claims claims = parseToken(token);
        return new AuthUser(claims.getSubject(), claims.get("name", String.class));
    }

    private SecretKey buildSecretKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException | DecodingException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
