package dev.hazoe.audiostreaming.audio.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioStreamResponse;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.common.exception.AudioStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AudioStreamServiceTest {

    @Mock
    private AudioRepository audioRepository;

    private AudioStreamService audioStreamService;

    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        audioStreamService = new AudioStreamService(audioRepository);

        tempDir = Files.createTempDirectory("audio-test");

        // inject @Value field manually
        ReflectionTestUtils.setField(
                audioStreamService,
                "storageRoot",
                tempDir.toString()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private Path createTestAudioFile(String name, int size) throws IOException {
        Path file = tempDir.resolve(name);
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 255);
        }
        Files.write(file, data);
        return file;
    }

    @Test
    void stream_fullAudio_success() throws Exception {
        // given
        Path audioFile = createTestAudioFile("test.mp3", 1024);
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setAudioPath("test.mp3");

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        AudioStreamResponse response =
                audioStreamService.stream(1L, null);

        // then
        assertThat(response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.headers().getFirst(HttpHeaders.CONTENT_LENGTH))
                .isEqualTo("1024");

        assertThat(response.headers().getFirst(HttpHeaders.CONTENT_RANGE))
                .isNull();

        assertThat(response.contentType()).isNotBlank();
        assertThat(response.resource()).isNotNull();
    }

    @Test
    void stream_partialAudio_success() throws Exception {
        // given
        Path audioFile = createTestAudioFile("test.mp3", 2000);
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setAudioPath("test.mp3");

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        AudioStreamResponse response =
                audioStreamService.stream(1L, "bytes=0-999");

        // then
        assertThat(response.status()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.headers().getFirst(HttpHeaders.CONTENT_LENGTH))
                .isEqualTo("1000");
        assertThat(response.headers().getFirst(HttpHeaders.CONTENT_RANGE))
                .isEqualTo("bytes 0-999/2000");
    }

    @Test
    void stream_audioNotFound_throwsException() {
        given(audioRepository.findById(1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                audioStreamService.stream(1L, null)
        ).isInstanceOf(AudioNotFoundException.class);
    }

    @Test
    void stream_fileNotExists_throwsException() {
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setAudioPath("missing.mp3");

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        assertThatThrownBy(() ->
                audioStreamService.stream(1L, null)
        ).isInstanceOf(AudioStorageException.class)
                .hasMessageContaining("not accessible");
    }

    @Test
    void stream_invalidRange_throwsException() throws Exception {
        Path audioFile = createTestAudioFile("test.mp3", 500);
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setAudioPath("test.mp3");

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        assertThatThrownBy(() ->
                audioStreamService.stream(1L, "bytes=9999-10000")
        ).isInstanceOf(AudioStorageException.class)
                .hasMessageContaining("Invalid byte range");
    }

}