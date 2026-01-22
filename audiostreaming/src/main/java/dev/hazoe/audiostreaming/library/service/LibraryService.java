package dev.hazoe.audiostreaming.library.service;

import dev.hazoe.audiostreaming.library.dto.LibraryItemDto;
import dev.hazoe.audiostreaming.library.mapper.LibraryItemMapper;
import dev.hazoe.audiostreaming.library.repository.LibraryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final LibraryItemRepository libraryItemRepository;
    private final LibraryItemMapper libraryItemMapper;

    @Transactional(readOnly = true)
    public List<LibraryItemDto> list(Long userId) {
        return libraryItemRepository.findByUserIdWithAudio(userId)
                .stream()
                .map(this.libraryItemMapper::toDto)
                .toList();
    }
}
