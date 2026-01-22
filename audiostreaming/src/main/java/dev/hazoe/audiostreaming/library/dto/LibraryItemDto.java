package dev.hazoe.audiostreaming.library.dto;

public record LibraryItemDto(
        Long id,
        String title,
        int durationSeconds,
        boolean premium) {

}
