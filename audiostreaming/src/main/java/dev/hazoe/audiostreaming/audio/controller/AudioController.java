package dev.hazoe.audiostreaming.audio.controller;

import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.dto.AudioStreamResponse;
import dev.hazoe.audiostreaming.audio.service.AudioService;
import dev.hazoe.audiostreaming.audio.service.AudioStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audios")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;
    private final AudioStreamService streamService;

    @GetMapping
    public ResponseEntity<Page<AudioListItemDto>> listAudios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity
                .ok()
                .body(
                        audioService.getAudios(
                                PageRequest.of(
                                        page,
                                        size,
                                        Sort.by("createdAt").descending())
                        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AudioDetailDto> getAudio(@PathVariable Long id) {
        return ResponseEntity
                .ok()
                .body(audioService.getAudioDetail(id));
    }

    @GetMapping("/{id}/stream")
    @PreAuthorize("@audioAuth.canStream(#id, authentication)")
    public ResponseEntity<Resource> stream(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range,
            Authentication authentication
    ) {
        AudioStreamResponse response = streamService.stream(id, range);

        return ResponseEntity
                .status(response.status())
                .headers(response.headers())
                .contentType(MediaType.parseMediaType(response.contentType()))
                .body(response.resource());
    }

}

