package dev.hazoe.audiostreaming.library.domain;

import dev.hazoe.audiostreaming.audio.domain.Audio;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false)
    private Audio audio;

    @Column(name = "saved_at")
    private Instant savedAt = Instant.now();


}
