package dev.hazoe.audiostreaming.audio.streaming;

import dev.hazoe.audiostreaming.common.exception.RangeNotSatisfiableException;

public class RangeResolver {

    private final long defaultChunkSize;

    public RangeResolver(long defaultChunkSize) {
        this.defaultChunkSize = defaultChunkSize;
    }

    public ByteRange resolve(String rangeHeader, long fileSize) {

        // No Range → full content
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return new ByteRange(
                    0,
                    fileSize - 1,
                    fileSize,
                    false
            );
        }

        try {
            String value = rangeHeader.substring(6);
            String[] parts = value.split("-", 2);

            long start;
            long end;

            // bytes=-500  (last 500 bytes)
            if (parts[0].isBlank()) {
                long suffixLength = Long.parseLong(parts[1]);

                if (suffixLength <= 0) {
                    throw new IllegalArgumentException();
                }

                start = Math.max(fileSize - suffixLength, 0);
                end = fileSize - 1;
            }
            // bytes=500- OR bytes=500-999
            else {
                start = Long.parseLong(parts[0]);

                end = (parts.length > 1 && !parts[1].isBlank())
                        ? Long.parseLong(parts[1])
                        : Math.min(start + defaultChunkSize - 1, fileSize - 1);
            }

            if (start < 0 || start >= fileSize || end >= fileSize || start > end) {
                throw new IllegalArgumentException();
            }

            return new ByteRange(
                    start,
                    end,
                    end - start + 1,
                    true
            );

        } catch (IllegalArgumentException ex) {
            throw new RangeNotSatisfiableException(
                    "Invalid Range header: " + rangeHeader,
                    fileSize
            );
        }
    }
}
