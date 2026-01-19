package dev.hazoe.audiostreaming.audio.controller;

import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audios")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

    @GetMapping
    public Page<AudioListItemDto> listAudios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return audioService.getAudios(
                PageRequest.of(
                        page,
                        size,
                        Sort.by("createdAt").descending())
        );
    }

    @GetMapping("/{id}")
    public AudioDetailDto getAudio(@PathVariable Long id) {
        return audioService.getAudioDetail(id);
    }
}

