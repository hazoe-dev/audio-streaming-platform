package dev.hazoe.audiostreaming.audio.repository;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioRepository extends JpaRepository<Audio, Long> {
}

