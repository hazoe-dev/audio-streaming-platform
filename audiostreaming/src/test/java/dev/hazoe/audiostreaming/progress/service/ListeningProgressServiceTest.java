package dev.hazoe.audiostreaming.progress.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.common.exception.InvalidProgressPositionException;
import dev.hazoe.audiostreaming.progress.domain.ListeningProgress;
import dev.hazoe.audiostreaming.progress.dto.ListeningProgressResponse;
import dev.hazoe.audiostreaming.progress.repository.ListeningProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListeningProgressServiceTest {

    @Mock
    private ListeningProgressRepository progressRepository;

    @Mock
    private AudioRepository audioRepository;

    @InjectMocks
    private ListeningProgressService service;

    /* ================= SAVE ================= */

    @Test
    void saveProgress_whenAudioNotFound_shouldThrowException() {
        when(audioRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                AudioNotFoundException.class,
                () -> service.saveProgress(1L, 10L, 50)
        );

        verifyNoInteractions(progressRepository);
    }

    @Test
    void saveProgress_whenPositionExceedsDuration_shouldThrowException() {
        Audio audio = mock(Audio.class);
        when(audio.getDurationSeconds()).thenReturn(100);
        when(audioRepository.findById(10L)).thenReturn(Optional.of(audio));

        assertThrows(
                InvalidProgressPositionException.class,
                () -> service.saveProgress(1L, 10L, 150)
        );

        verifyNoInteractions(progressRepository);
    }

    @Test
    void saveProgress_whenProgressExists_shouldUpdateWithoutSave() {
        Audio audio = mock(Audio.class);
        when(audio.getDurationSeconds()).thenReturn(300);
        when(audioRepository.findById(10L)).thenReturn(Optional.of(audio));

        ListeningProgress progress = mock(ListeningProgress.class);
        when(progressRepository.findByUserIdAndAudioId(1L, 10L))
                .thenReturn(Optional.of(progress));

        service.saveProgress(1L, 10L, 120);

        verify(progress).updatePosition(120);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void saveProgress_whenProgressDoesNotExist_shouldSaveOnceAndUpdate() {
        Audio audio = mock(Audio.class);
        when(audio.getDurationSeconds()).thenReturn(300);
        when(audioRepository.findById(10L)).thenReturn(Optional.of(audio));

        when(progressRepository.findByUserIdAndAudioId(1L, 10L))
                .thenReturn(Optional.empty());

        ListeningProgress saved = mock(ListeningProgress.class);
        when(progressRepository.save(any(ListeningProgress.class)))
                .thenReturn(saved);

        service.saveProgress(1L, 10L, 120);

        verify(progressRepository).save(any(ListeningProgress.class));
        verify(saved).updatePosition(120);
    }

    /* ================= GET ================= */

    @Test
    void getProgress_whenProgressExists_shouldReturnSavedPosition() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        Audio audio = mock(Audio.class);
        ListeningProgress progress = mock(ListeningProgress.class);

        when(audioRepository.findById(audioId))
                .thenReturn(Optional.of(audio));
        when(progressRepository.findByUserIdAndAudioId(userId, audioId))
                .thenReturn(Optional.of(progress));
        when(progress.getPositionSeconds())
                .thenReturn(45);

        // when
        ListeningProgressResponse response =
                service.getProgress(userId, audioId);

        // then
        assertThat(response.audioId()).isEqualTo(audioId);
        assertThat(response.positionSeconds()).isEqualTo(45);
    }

    @Test
    void getProgress_whenNoProgressExists_shouldReturnZeroPosition() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        Audio audio = mock(Audio.class);

        when(audioRepository.findById(audioId))
                .thenReturn(Optional.of(audio));
        when(progressRepository.findByUserIdAndAudioId(userId, audioId))
                .thenReturn(Optional.empty());

        // when
        ListeningProgressResponse response =
                service.getProgress(userId, audioId);

        // then
        assertThat(response.audioId()).isEqualTo(audioId);
        assertThat(response.positionSeconds()).isZero();
    }

    @Test
    void getProgress_whenAudioNotFound_shouldThrowException() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        when(audioRepository.findById(audioId))
                .thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() ->
                service.getProgress(userId, audioId)
        )
                .isInstanceOf(AudioNotFoundException.class)
                .hasMessageContaining(audioId.toString());

        verifyNoInteractions(progressRepository);
    }

}
