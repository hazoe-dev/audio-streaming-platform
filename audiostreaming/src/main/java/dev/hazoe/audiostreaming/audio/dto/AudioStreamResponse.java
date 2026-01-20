package dev.hazoe.audiostreaming.audio.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;


public record AudioStreamResponse(
        Resource resource,
        HttpHeaders headers,
        HttpStatus status,
        String contentType
) {}
