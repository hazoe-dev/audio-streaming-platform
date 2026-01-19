package dev.hazoe.audiostreaming.audio.dto;

public record AudioDetailDto(
        Long id,
        String title,
        String description,
        int durationSeconds,
        String coverUrl,
        boolean isPremium
) {
}
