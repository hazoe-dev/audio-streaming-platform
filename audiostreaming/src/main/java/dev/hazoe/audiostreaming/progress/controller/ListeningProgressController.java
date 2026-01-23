package dev.hazoe.audiostreaming.progress.controller;

import dev.hazoe.audiostreaming.common.security.UserPrincipal;
import dev.hazoe.audiostreaming.progress.dto.ListeningProgressResponse;
import dev.hazoe.audiostreaming.progress.dto.SaveProgressRequest;
import dev.hazoe.audiostreaming.progress.service.ListeningProgressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ListeningProgressController {
    private final ListeningProgressService progressService;

    @PutMapping
    public ResponseEntity<Void> saveProgress(@Valid @RequestBody SaveProgressRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        progressService.saveProgress(
                principal.getUserId(),
                request.audioId(),
                request.positionSeconds()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{audioId}")
    public ResponseEntity<ListeningProgressResponse> getProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long audioId) {
        return ResponseEntity.ok(
                progressService.getProgress(principal.getUserId(), audioId)
        );
    }
}
