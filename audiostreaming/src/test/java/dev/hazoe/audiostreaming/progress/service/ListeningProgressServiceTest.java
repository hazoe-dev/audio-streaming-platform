package dev.hazoe.audiostreaming.progress.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.common.exception.InvalidProgressPositionException;
import dev.hazoe.audiostreaming.progress.domain.ListeningProgress;
import dev.hazoe.audiostreaming.progress.repository.ListeningProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
}
