package dev.hazoe.audiostreaming.library.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import dev.hazoe.audiostreaming.library.domain.LibraryItem;
import dev.hazoe.audiostreaming.library.dto.LibraryItemDto;
import dev.hazoe.audiostreaming.library.mapper.LibraryItemMapper;
import dev.hazoe.audiostreaming.library.repository.LibraryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @InjectMocks
    private LibraryService libraryService;

    @Mock
    private LibraryItemMapper libraryItemMapper;

    @Mock
    private AudioRepository audioRepository;

    @Test
    void list_shouldReturnLibraryItems_whenAuthenticatedUser() {
        // given
        Long userId = 42L;

        Audio audio1 = Audio.builder()
                .id(1L)
                .title("Mindful Focus")
                .durationSeconds(300)
                .isPremium(false)
                .build();

        Audio audio2 = Audio.builder()
                .id(2L)
                .title("Deep Sleep")
                .durationSeconds(600)
                .isPremium(true)
                .build();

        LibraryItem item1 = LibraryItem.builder()
                .audio(audio1)
                .build();

        LibraryItem item2 = LibraryItem.builder()
                .audio(audio2)
                .build();

        when(libraryItemRepository.findByUserIdWithAudio(userId))
                .thenReturn(List.of(item1, item2));

        LibraryItemDto dto1 = new LibraryItemDto(
                1L, "Mindful Focus", 300, false
        );
        LibraryItemDto dto2 = new LibraryItemDto(
                2L, "Deep Sleep", 600, true
        );

        when(libraryItemMapper.toDto(item1)).thenReturn(dto1);
        when(libraryItemMapper.toDto(item2)).thenReturn(dto2);

        // when
        List<LibraryItemDto> result = libraryService.list(userId);

        // then
        assertThat(result)
                .containsExactly(dto1, dto2);

        verify(libraryItemRepository).findByUserIdWithAudio(userId);
        verify(libraryItemMapper).toDto(item1);
        verify(libraryItemMapper).toDto(item2);
        verifyNoMoreInteractions(libraryItemRepository, libraryItemMapper);
    }


    @Test
    void list_shouldReturnEmptyList_whenUserHasNoLibraryItems() {
        // given
        Long userId = 1l;
        when(libraryItemRepository.findByUserIdWithAudio(userId))
                .thenReturn(List.of());

        // when
        List<LibraryItemDto> result = libraryService.list(userId);

        // then
        assertThat(result).isEmpty();
        verify(libraryItemRepository).findByUserIdWithAudio(userId);
        verifyNoInteractions(libraryItemMapper);
    }

    @Test
    void save_shouldPersistLibraryItem_whenAudioExistsAndNotInLibrary() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        Audio audio = Audio.builder()
                .id(audioId)
                .build();

        when(audioRepository.findById(audioId))
                .thenReturn(Optional.of(audio));

        when(libraryItemRepository.existsByUserIdAndAudio_Id(userId, audioId))
                .thenReturn(false);

        // when
        libraryService.save(userId, audioId);

        // then
        verify(audioRepository).findById(audioId);
        verify(libraryItemRepository)
                .existsByUserIdAndAudio_Id(userId, audioId);

        verify(libraryItemRepository).save(
                argThat(item ->
                        item.getUserId().equals(userId) &&
                                item.getAudio().equals(audio)
                )
        );
    }

    @Test
    void save_shouldDoNothing_whenLibraryItemAlreadyExists() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        Audio audio = Audio.builder()
                .id(audioId)
                .build();

        when(audioRepository.findById(audioId))
                .thenReturn(Optional.of(audio));

        when(libraryItemRepository.existsByUserIdAndAudio_Id(userId, audioId))
                .thenReturn(true);

        // when
        libraryService.save(userId, audioId);

        // then
        verify(audioRepository).findById(audioId);
        verify(libraryItemRepository)
                .existsByUserIdAndAudio_Id(userId, audioId);

        verify(libraryItemRepository, never())
                .save(any());
    }

    @Test
    void save_shouldThrowAudioNotFoundException_whenAudioDoesNotExist() {
        // given
        Long userId = 1L;
        Long audioId = 99L;

        when(audioRepository.findById(audioId))
                .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> libraryService.save(userId, audioId))
                .isInstanceOf(AudioNotFoundException.class)
                .hasMessageContaining(audioId.toString());

        verify(audioRepository).findById(audioId);
        verifyNoInteractions(libraryItemRepository);
    }

}
