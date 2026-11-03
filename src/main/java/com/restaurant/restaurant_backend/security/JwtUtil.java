package com.restaurant.restaurant_backend.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey key;

    private final long expirationMs = 1000 * 60 * 60 * 8; // 8 hours

    // The signing key now comes from a setting (jwt.secret), never from the code
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String subjectName, Long restaurantId, String role) {
        return Jwts.builder()
                .subject(subjectName)
                .claim("restaurantId", restaurantId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public Long extractRestaurantId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Object value = claims.get("restaurantId");
        return value != null ? Long.valueOf(value.toString()) : null;
    }

    public String extractRole(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Object value = claims.get("role");
        return value != null ? value.toString() : null;
    }
}