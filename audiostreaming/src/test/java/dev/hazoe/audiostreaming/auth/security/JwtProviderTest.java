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

    private static final long ACCESS_EXP = 3600;
    private static final long REFRESH_EXP = 86400;
    private static final String ISSUER = "audiostreaming-auth";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessExpiration", ACCESS_EXP);
        ReflectionTestUtils.setField(jwtProvider, "refreshExpiration", REFRESH_EXP);
        ReflectionTestUtils.setField(jwtProvider, "issuer", ISSUER);
    }

    // Ex: "qn0FJ7ZpPqK4Zz0cZ2P7z3y9w6Z2o8k1v1Qv8Z9l5QY="
    private static String genSecretKey() {
        return Encoders.BASE64.encode(
                Jwts.SIG.HS256.key().build().getEncoded()
        );
    }

    /* ================= ACCESS TOKEN ================= */

    @Test
    void generateAccessToken_andParseToken_shouldReturnValidClaims() {
        // given
        Long userId = 1L;
        String role = "PREMIUM";

        // when
        String token = jwtProvider.generateAccessToken(userId, role);

        // then
        Claims claims = jwtProvider.parseClaims(token);
        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role")).isEqualTo("PREMIUM");

        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().toInstant())
                .isAfter(claims.getIssuedAt().toInstant());
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.get("typ")).isEqualTo("ACCESS");
        assertThat(claims.get("role")).isEqualTo("PREMIUM");
    }

    @Test
    void generateAccessToken_shouldHaveCorrectExpiration() {
        // given
        Instant before = Instant.now();

        // when
        String token = jwtProvider.generateAccessToken(1L, "ADMIN");
        Claims claims = jwtProvider.parseClaims(token);

        Instant exp = claims.getExpiration().toInstant();

        // then
        assertThat(exp)
                .isBetween(
                        before.plusSeconds(ACCESS_EXP - 5),
                        before.plusSeconds(ACCESS_EXP + 5)
                );
    }

    /* ================= REFRESH TOKEN ================= */
    @Test
    void generateRefreshToken_shouldHaveRefreshType() {
        // when
        String token = jwtProvider.generateRefreshToken(99L);

        // then
        Claims claims = jwtProvider.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("99");
        assertThat(claims.get("typ")).isEqualTo("REFRESH");
    }

    @Test
    void validateRefreshToken_shouldReturnTrue_forValidRefreshToken() {
        // given
        String token = jwtProvider.generateRefreshToken(1L);

        // when + then
        assertThat(jwtProvider.validateRefreshToken(token)).isTrue();
    }

    /* ================= PARSING & VALIDATION ================= */
    @Test
    void parseClaims_shouldThrowException_whenTokenIsTampered() {
        // given
        String token = jwtProvider.generateAccessToken(1L, "FREE");

        // modify token
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        // when + then
        assertThatThrownBy(() -> jwtProvider.parseClaims(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseClaims_shouldThrowException_whenIssuerIsWrong() {
        String token = Jwts.builder()
                .subject("1")
                .issuer("evil-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        assertThatThrownBy(() -> jwtProvider.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }

    /* ================= PRINCIPAL EXTRACTION ================= */

    @Test
    void getPrincipalFromToken_shouldReturnCorrectUserPrincipal() {
        // given
        Long userId = 10L;
        String role = "ADMIN";

        String token = jwtProvider.generateAccessToken(userId, role);

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
    void getPrincipalFromToken_shouldThrowException_whenRoleMissing() {
        String token = Jwts.builder()
                .subject("1")
                .issuer(ISSUER)
                .claim("typ", "ACCESS")
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