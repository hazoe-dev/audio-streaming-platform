package dev.hazoe.audiostreaming.search.service;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.service.expose.AudioQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudioSearchService {

    private final AudioQueryService audioQueryService;

    public Page<AudioListItemDto> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }

        String tsQuery = toTsQuery(keyword);

        return audioQueryService.search(tsQuery, pageable);
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

