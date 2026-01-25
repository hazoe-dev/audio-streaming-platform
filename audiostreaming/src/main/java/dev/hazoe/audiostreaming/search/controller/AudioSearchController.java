package dev.hazoe.audiostreaming.search.controller;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.search.service.AudioSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audios")
@RequiredArgsConstructor
public class AudioSearchController {
    private final AudioSearchService searchService;

    @GetMapping("/search")
    public Page<AudioListItemDto> search(
            @RequestParam("q") String keyword,
            Pageable pageable
    ) {
        return searchService.search(keyword, pageable);
    }

}
