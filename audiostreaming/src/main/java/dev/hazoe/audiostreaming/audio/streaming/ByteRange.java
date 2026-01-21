package dev.hazoe.audiostreaming.audio.streaming;

public record ByteRange(
        long start,
        long end,
        long contentLength,
        boolean partial
) {}
