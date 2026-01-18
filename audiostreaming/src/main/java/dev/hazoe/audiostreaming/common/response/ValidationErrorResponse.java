package dev.hazoe.audiostreaming.common.response;

import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String message,
        Map<String, String> errors
) {}
