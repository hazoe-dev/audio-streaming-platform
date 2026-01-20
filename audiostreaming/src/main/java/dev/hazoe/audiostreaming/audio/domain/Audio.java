package dev.hazoe.audiostreaming.audio.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Audio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int durationSeconds;

    @Column(nullable = false)
    private String audioPath;

    @Column(nullable = false, length = 100)
    private String contentType ;

    private String coverPath;

    @Column(nullable = false)
    private boolean isPremium;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();

        if (this.contentType == null) {
            this.contentType = "audio/mpeg";
        }
    }

}
