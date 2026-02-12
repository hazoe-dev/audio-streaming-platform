package dev.hazoe.audiostreaming.library.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "library_item",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "audio_id"})
        }
)
public class LibraryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "audio_id", nullable = false)
    private Long audioId;

    @Column(name = "saved_at")
    private Instant savedAt;

    public LibraryItem(Long userId, Long audioId) {
        this.userId = userId;
        this.audioId = audioId;
        savedAt = Instant.now();
    }
}
