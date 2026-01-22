package dev.hazoe.audiostreaming.library.mapper;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.library.domain.LibraryItem;
import dev.hazoe.audiostreaming.library.dto.LibraryItemDto;
import org.springframework.stereotype.Component;

@Component
public class LibraryItemMapper {

    public LibraryItemDto toDto(LibraryItem item) {
        Audio audio = item.getAudio();
        return new LibraryItemDto(
                audio.getId(),
                audio.getTitle(),
                audio.getDurationSeconds(),
                audio.isPremium()
        );
    }

}
