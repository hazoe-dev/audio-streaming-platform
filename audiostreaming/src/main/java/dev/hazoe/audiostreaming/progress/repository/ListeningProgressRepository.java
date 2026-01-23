package dev.hazoe.audiostreaming.progress.repository;

import dev.hazoe.audiostreaming.progress.domain.ListeningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ListeningProgressRepository extends JpaRepository<ListeningProgress, Long> {

    Optional<ListeningProgress> findByUserIdAndAudioId(Long userId, Long audioId);
}
