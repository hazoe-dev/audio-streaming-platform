package dev.hazoe.audiostreaming.library.service;

import dev.hazoe.audiostreaming.audio.service.expose.AudioQueryService;
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
    private final AudioQueryService audioQueryService;
    private final LibraryItemMapper libraryItemMapper;

    @Transactional(readOnly = true)
    public List<LibraryItemDto> list(Long userId) {
        List<LibraryItem> items = libraryItemRepository.findByUserId(userId);
        List<Long> audioIds = items.stream().map(LibraryItem::getAudioId).toList();
        return audioQueryService.getSummaryList(audioIds)
                .stream()
                .map(this.libraryItemMapper::toDto)
                .toList();

    }

    @Transactional
    public void save(Long userId, Long audioId) {
        if (!audioQueryService.existsById(audioId)) {
            throw new AudioNotFoundException(audioId);
        }
        if (libraryItemRepository.existsByUserIdAndAudioId(userId, audioId)) {
            return;
        }
        libraryItemRepository.save(new LibraryItem(userId, audioId));
    }

    @Transactional
    public void delete(Long userId, Long audioId) {
        libraryItemRepository.deleteByUserIdAndAudioId(userId, audioId);
    }
}
