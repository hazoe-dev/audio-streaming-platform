package dev.hazoe.audiostreaming.search.service;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.mapper.AudioMapper;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudioSearchService {

    private final AudioRepository audioRepository;
    private final AudioMapper audioMapper;

    public Page<AudioListItemDto> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }

        String tsQuery = toTsQuery(keyword);

        return audioRepository.search(tsQuery, pageable)
                .map(audioMapper::toListItem);
    }

    private String toTsQuery(String keyword) {
        return Arrays.stream(keyword.trim().split("\\s+"))
                .map(this::escapeTsTerm)
                .map(term -> term + ":*")
                .collect(Collectors.joining(" | "));
    }

    /**
     * Optional but recommended:
     * Prevent tsquery syntax errors if keyword contains special characters.
     */
    private String escapeTsTerm(String term) {
        return term.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
    }
}

