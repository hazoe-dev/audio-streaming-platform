package dev.hazoe.audiostreaming.progress.dto;

public record ListeningProgressResponse(
        Long audioId,
        int positionSeconds
) {
}
