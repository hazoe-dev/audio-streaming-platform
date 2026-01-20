package dev.hazoe.audiostreaming.audio.service;


import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioStreamResponse;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;

import dev.hazoe.audiostreaming.audio.streaming.ByteRange;
import dev.hazoe.audiostreaming.audio.streaming.RangeResolver;
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

    private final RangeResolver rangeResolver =
            new RangeResolver(DEFAULT_CHUNK_SIZE);

    @Value("${app.cdn.audio-base-url}")
    private String storageRoot;

    public AudioStreamResponse stream(Long audioId, String rangeHeader) {

        // 1. Load audio metadata
        Audio audio = audioRepository.findById(audioId)
                .orElseThrow(() -> new AudioNotFoundException(audioId));

        // 2. Validate storage
        Path path = Path.of(storageRoot, audio.getAudioPath());
        validateFile(path);

        // 3. Get content type
        String contentType = audio.getContentType();

        // 4. Parse Range
        long fileSize = getFileSize(path);
        ByteRange range = rangeResolver.resolve(rangeHeader, fileSize);

        // 5. Build streaming resource
        Resource resource = new InputStreamResource(openStream(path, range.start(), range.contentLength()));

        // 6. Build headers
        HttpHeaders headers = buildHeaders(range, fileSize);

        return new AudioStreamResponse(
                resource,
                headers,
                range.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK,
                contentType
        );
    }

    private HttpHeaders buildHeaders(ByteRange range, long fileSize) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(range.contentLength()));

        if (range.partial()) {
            headers.set(
                    HttpHeaders.CONTENT_RANGE,
                    "bytes %d-%d/%d".formatted(range.start(), range.end(), fileSize)
            );
        }
        return headers;
    }

    private long getFileSize(Path path) {
        long fileSize;
        try {
            fileSize = Files.size(path);
        } catch (IOException e) {
            throw new AudioStorageException("Failed to read file size: " + path, e);
        }
        return fileSize;
    }

    private void validateFile(Path path) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new AudioStorageException("Audio file not accessible: " + path);
        }
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
