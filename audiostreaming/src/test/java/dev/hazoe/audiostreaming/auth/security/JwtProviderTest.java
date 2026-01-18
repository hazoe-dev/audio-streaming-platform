package dev.hazoe.audiostreaming.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    // Base64 key >= 256 bits
    private static final String SECRET = genSecretKey();

    private static final long EXPIRATION_SECONDS = 3600;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "expiration", EXPIRATION_SECONDS);
    }

    // Ex: "qn0FJ7ZpPqK4Zz0cZ2P7z3y9w6Z2o8k1v1Qv8Z9l5QY="
    private static String genSecretKey() {
        return Encoders.BASE64.encode(
                Jwts.SIG.HS256.key().build().getEncoded()
        );
    }

    @Test
    void generateAndParseToken_shouldReturnValidClaims() {
        // given
        Long userId = 1L;
        String role = "PREMIUM";

        // when
        String token = jwtProvider.generateToken(userId, role);

        Claims claims = jwtProvider.parseClaims(token);

        // then
        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role")).isEqualTo("PREMIUM");

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().toInstant())
                .isAfter(claims.getIssuedAt().toInstant());
    }

    @Test
    void parseClaims_shouldThrowException_whenTokenIsTampered() {
        // given
        String token = jwtProvider.generateToken(1L, "FREE");

        // modify token
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        // when + then
        assertThatThrownBy(() -> jwtProvider.parseClaims(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void generateToken_shouldHaveCorrectExpiration() {
        // given
        Instant before = Instant.now();

        // when
        String token = jwtProvider.generateToken(1L, "ADMIN");
        Claims claims = jwtProvider.parseClaims(token);

        Instant exp = claims.getExpiration().toInstant();

        // then
        assertThat(exp)
                .isBetween(
                        before.plusSeconds(EXPIRATION_SECONDS - 5),
                        before.plusSeconds(EXPIRATION_SECONDS + 5)
                );
    }

    @Test
    void getPrincipalFromToken_shouldReturnCorrectUserPrincipal() {
        // given
        Long userId = 10L;
        String role = "ADMIN";

        String token = jwtProvider.generateToken(userId, role);

        // when
        UserPrincipal principal = jwtProvider.getPrincipalFromToken(token);

        // then
        assertThat(principal).isNotNull();
        assertThat(principal.getUsername()).isEqualTo(userId.toString());
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void getPrincipalFromToken_shouldThrowException_whenTokenInvalid() {
        String invalidToken = "invalid.jwt.token";

        assertThatThrownBy(() ->
                jwtProvider.getPrincipalFromToken(invalidToken)
        ).isInstanceOf(Exception.class);
    }

    @Test
    void getPrincipalFromToken_shouldFail_whenRoleMissing() {
        String token = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        assertThatThrownBy(() ->
                jwtProvider.getPrincipalFromToken(token)
        ).isInstanceOf(JwtException.class)
                .hasMessageContaining("Missing role");
    }

}