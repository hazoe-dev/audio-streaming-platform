package dev.hazoe.audiostreaming.auth.repository;

import dev.hazoe.audiostreaming.auth.domain.RefreshToken;
import dev.hazoe.audiostreaming.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
