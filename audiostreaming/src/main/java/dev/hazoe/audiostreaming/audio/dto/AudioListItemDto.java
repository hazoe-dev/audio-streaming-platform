package dev.hazoe.audiostreaming.audio.dto;

public record AudioListItemDto(
        Long id,
        String title,
        int durationSeconds,
        boolean isPremium
) {
}
