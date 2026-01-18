package dev.hazoe.audiostreaming.common.exception.dto;

public record ApiErrorResponse(
        int status,
        String error,
        String message
) {}

