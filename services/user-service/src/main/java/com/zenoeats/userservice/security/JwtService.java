package com.zenoeats.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    @Getter
    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMs))
            .signWith(privateKey)   // JJWT auto-selects RS256 for RSAPrivateKey
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractClaims(token);
        return claims.getSubject().equals(userDetails.getUsername())
            && claims.getExpiration().after(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(publicKey)  // only the public key is needed to verify
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
