package dev.hazoe.audiostreaming.library.controller;

import dev.hazoe.audiostreaming.common.security.UserPrincipal;
import dev.hazoe.audiostreaming.library.dto.LibraryItemDto;
import dev.hazoe.audiostreaming.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<LibraryItemDto>> list(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(libraryService.list(userId));
    }
}
