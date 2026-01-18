package dev.hazoe.audiostreaming.common.exception.dto;

import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String message,
        Map<String, String> errors
) {}
