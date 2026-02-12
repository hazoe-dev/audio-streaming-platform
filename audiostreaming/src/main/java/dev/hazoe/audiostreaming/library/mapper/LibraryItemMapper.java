package dev.hazoe.audiostreaming.library.mapper;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.library.dto.LibraryItemDto;
import org.springframework.stereotype.Component;

@Component
public class LibraryItemMapper {

    public LibraryItemDto toDto(AudioListItemDto item) {
        return new LibraryItemDto(
                item.id(),
                item.title(),
                item.durationSeconds(),
                item.isPremium()
        );
    }

}
