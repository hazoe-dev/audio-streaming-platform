package dev.hazoe.audiostreaming.auth.service;

import dev.hazoe.audiostreaming.auth.domain.RefreshToken;
import dev.hazoe.audiostreaming.auth.domain.User;
import dev.hazoe.audiostreaming.auth.repository.RefreshTokenRepository;
import dev.hazoe.audiostreaming.auth.security.JwtProvider;
import dev.hazoe.audiostreaming.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    /* ================= CREATE ================= */

    public String create(User user) {
        String token = jwtProvider.generateRefreshToken(user.getId());
        Instant expiresAt = jwtProvider.extractExpiration(token);

        RefreshToken entity = new RefreshToken();
        entity.setToken(token);
        entity.setUser(user);
        entity.setExpiresAt(expiresAt);

        refreshTokenRepository.save(entity);
        return token;
    }

    /* ================= VALIDATE ================= */

    public RefreshToken validate(String refreshToken) {

        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token expired");
        }

        return stored;
    }

    /* ================= ROTATE ================= */

    @Transactional
    public String rotate(User user) {
        refreshTokenRepository.deleteByUser(user);
        return create(user);
    }

    /* ================= REVOKE ================= */

    public void revoke(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
