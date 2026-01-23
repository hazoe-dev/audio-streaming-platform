package dev.hazoe.audiostreaming.progress.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "listening_progress",
        uniqueConstraints = {
        @UniqueConstraint(name= "uq_progress_user_audio", columnNames = {"user_id", "audio_id"})
})
public class ListeningProgress {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "audio_id", nullable = false)
    private Long audioId;

    @Column(name = "position_seconds", nullable = false)
    private Integer positionSeconds;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ListeningProgress(Long userId, Long audioId, Integer positionSeconds) {
        this.userId = userId;
        this.audioId = audioId;
        this.positionSeconds = positionSeconds;
        this.updatedAt = Instant.now();
    }

    public void updatePosition(int positionSeconds) {
        this.positionSeconds = positionSeconds;
        this.updatedAt = Instant.now();
    }
}
