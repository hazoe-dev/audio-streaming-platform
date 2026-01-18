package dev.hazoe.audiostreaming.common.response;

public record ApiErrorResponse(
        int status,
        String error,
        String message
) {}

