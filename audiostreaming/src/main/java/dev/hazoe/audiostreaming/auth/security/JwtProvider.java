package dev.hazoe.audiostreaming.auth.security;

import dev.hazoe.audiostreaming.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${security.jwt.secret}")
    @NotNull
    private String secret;

    @Value("${security.jwt.access-expiration}")
    @NotNull
    private long accessExpiration;

    @Value("${security.jwt.refresh-expiration}")
    @NotNull
    private long refreshExpiration;

    @Value("${security.jwt.issuer}")
    @NotNull
    private String issuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /* ================= ACCESS TOKEN ================= */
    public String generateAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("typ", "ACCESS")
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpiration)))
                .signWith(getSigningKey())
                .compact();
    }

    /* ================= REFRESH TOKEN ================= */

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("typ", "REFRESH")
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpiration)))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "REFRESH".equals(claims.get("typ", String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /* ================= COMMON ================= */

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Instant extractExpiration(String token) {
        Claims claims = parseClaims(token); // verify signature
        return claims.getExpiration().toInstant();
    }

    public UserPrincipal getPrincipalFromToken(String token) {
        Claims claims = parseClaims(token);

        if (!"ACCESS".equals(claims.get("typ", String.class))) {
            throw new JwtException("Invalid token type");
        }

        String subject = claims.getSubject();
        if (subject == null) {
            throw new JwtException("Missing subject");
        }

        Long userId = Long.parseLong(subject);
        String role = claims.get("role", String.class);

        if (role == null) {
            throw new JwtException("Missing role claim");
        }

        return new UserPrincipal(userId, role);
    }
}
