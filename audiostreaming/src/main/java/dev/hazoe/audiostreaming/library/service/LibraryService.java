package dev.hazoe.audiostreaming.library.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.library.domain.LibraryItem;
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
    private final AudioRepository audioRepository;
    private final LibraryItemMapper libraryItemMapper;

    @Transactional(readOnly = true)
    public List<LibraryItemDto> list(Long userId) {
        return libraryItemRepository.findByUserIdWithAudio(userId)
                .stream()
                .map(this.libraryItemMapper::toDto)
                .toList();
    }

    @Transactional
    public void save(Long userId, Long audioId) {
        Audio audio = audioRepository.findById(audioId)
                .orElseThrow(() -> new AudioNotFoundException(audioId));
        if (libraryItemRepository.existsByUserIdAndAudio_Id(userId, audioId)) {
            return;
        }
        libraryItemRepository.save(new LibraryItem(userId, audio));
    }

    @Transactional
    public void delete(Long userId, Long audioId) {
        libraryItemRepository.deleteByUserIdAndAudio_Id(userId, audioId);
    }
}
