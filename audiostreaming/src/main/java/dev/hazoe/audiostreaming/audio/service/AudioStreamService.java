package dev.hazoe.audiostreaming.audio.service;


import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioStreamResponse;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;

import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.common.exception.AudioStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class AudioStreamService {

    private static final long DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1MB

    private final AudioRepository audioRepository;

    @Value("${app.cdn.audio-base-url}")
    private String storageRoot;

    public AudioStreamResponse stream(Long audioId, String rangeHeader) {

        // 1. Load audio metadata
        Audio audio = audioRepository.findById(audioId)
                .orElseThrow(() -> new AudioNotFoundException(audioId));

        Path path = Path.of(storageRoot, audio.getAudioPath());

        // 2. Validate storage
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new AudioStorageException("Audio file not accessible: " + path);
        }

        // 3. Resolve content type
        String contentType;
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            throw new AudioStorageException(
                    "Failed to determine content type for: " + path, e
            );
        }
        if (contentType == null) {
            contentType = "audio/mpeg";
        }

        long fileSize;
        try {
            fileSize = Files.size(path);
        } catch (IOException e) {
            throw new AudioStorageException("Failed to read file size: " + path, e);
        }

        // 4. Parse Range
        long start = 0;
        long end = fileSize - 1;
        boolean isPartial = false;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            isPartial = true;

            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(ranges[0]);

                if (ranges.length > 1 && !ranges[1].isBlank()) {
                    end = Long.parseLong(ranges[1]);
                } else {
                    end = Math.min(start + DEFAULT_CHUNK_SIZE - 1, fileSize - 1);
                }
            } catch (NumberFormatException ex) {
                throw new AudioStorageException("Invalid Range header: " + rangeHeader);
            }

            if (start >= fileSize || end >= fileSize || start > end) {
                throw new AudioStorageException("Invalid byte range: " + rangeHeader);
            }
        }

        long contentLength = end - start + 1;

        // 5. Build streaming resource
        Resource resource = new InputStreamResource(openStream(path, start, contentLength));

        // 6. Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));

        if (isPartial) {
            headers.set(
                    HttpHeaders.CONTENT_RANGE,
                    "bytes " + start + "-" + end + "/" + fileSize
            );
        }

        return new AudioStreamResponse(
                resource,
                headers,
                isPartial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK,
                contentType
        );
    }

    private InputStream openStream(Path path, long start, long length) {
        try {
            RandomAccessFile file = new RandomAccessFile(path.toFile(), "r");
            file.seek(start);

            return new InputStream() {
                private long remaining = length;

                @Override
                public int read() throws IOException {
                    if (remaining <= 0) return -1;
                    remaining--;
                    return file.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (remaining <= 0) return -1;
                    len = (int) Math.min(len, remaining);
                    int read = file.read(b, off, len);
                    if (read > 0) remaining -= read;
                    return read;
                }

                @Override
                public void close() throws IOException {
                    file.close();
                }
            };
        } catch (IOException e) {
            throw new AudioStorageException("Failed to open audio stream: " + path, e);
        }
    }
}
