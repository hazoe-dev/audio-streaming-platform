package dev.hazoe.audiostreaming.library.service;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.service.expose.AudioQueryService;
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
    private AudioQueryService audioQueryService;

    @Test
    void list_shouldReturnLibraryItems_whenAuthenticatedUser() {
        // given
        Long userId = 42L;

        AudioListItemDto audio1 = new AudioListItemDto(
                1L,
                "Mindful Focus",
                300,
                false
                );

        AudioListItemDto audio2 = new AudioListItemDto(
                1L,
                "Deep Sleep",
                600,
                true
        );


        LibraryItem item1 = LibraryItem.builder()
                .audioId(1l)
                .build();

        LibraryItem item2 = LibraryItem.builder()
                .audioId(2l)
                .build();

        when(libraryItemRepository.findByUserId(userId))
                .thenReturn(List.of(item1, item2));

        when(audioQueryService.getSummaryList(List.of(1l, 2l)))
                .thenReturn(List.of(audio1, audio2));

        LibraryItemDto dto1 = new LibraryItemDto(
                1L, "Mindful Focus", 300, false
        );
        LibraryItemDto dto2 = new LibraryItemDto(
                2L, "Deep Sleep", 600, true
        );

        when(libraryItemMapper.toDto(audio1)).thenReturn(dto1);
        when(libraryItemMapper.toDto(audio2)).thenReturn(dto2);

        // when
        List<LibraryItemDto> result = libraryService.list(userId);

        // then
        assertThat(result)
                .containsExactly(dto1, dto2);

        verify(libraryItemRepository).findByUserId(userId);
        verify(libraryItemMapper).toDto(audio1);
        verify(libraryItemMapper).toDto(audio2);
        verifyNoMoreInteractions(libraryItemRepository, libraryItemMapper);
    }


    @Test
    void list_shouldReturnEmptyList_whenUserHasNoLibraryItems() {
        // given
        Long userId = 1l;
        when(libraryItemRepository.findByUserId(userId))
                .thenReturn(List.of());

        // when
        List<LibraryItemDto> result = libraryService.list(userId);

        // then
        assertThat(result).isEmpty();
        verify(libraryItemRepository).findByUserId(userId);
        verifyNoInteractions(libraryItemMapper);
    }

    @Test
    void save_shouldPersistLibraryItem_whenAudioExistsAndNotInLibrary() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        when(audioQueryService.existsById(audioId))
                .thenReturn(true);

        when(libraryItemRepository.existsByUserIdAndAudioId(userId, audioId))
                .thenReturn(false);

        // when
        libraryService.save(userId, audioId);

        // then
        verify(audioQueryService).existsById(audioId);
        verify(libraryItemRepository)
                .existsByUserIdAndAudioId(userId, audioId);

        verify(libraryItemRepository).save(
                argThat(item ->
                        item.getUserId().equals(userId) &&
                                item.getAudioId().equals(audioId)
                )
        );
    }

    @Test
    void save_shouldDoNothing_whenLibraryItemAlreadyExists() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        when(audioQueryService.existsById(audioId))
                .thenReturn(true);

        when(libraryItemRepository.existsByUserIdAndAudioId(userId, audioId))
                .thenReturn(true);

        // when
        libraryService.save(userId, audioId);

        // then
        verify(audioQueryService).existsById(audioId);
        verify(libraryItemRepository)
                .existsByUserIdAndAudioId(userId, audioId);

        verify(libraryItemRepository, never())
                .save(any());
    }

    @Test
    void save_shouldThrowAudioNotFoundException_whenAudioDoesNotExist() {
        // given
        Long userId = 1L;
        Long audioId = 99L;

        when(audioQueryService.existsById(audioId))
                .thenReturn(false);

        // when / then
        assertThatThrownBy(() -> libraryService.save(userId, audioId))
                .isInstanceOf(AudioNotFoundException.class)
                .hasMessageContaining(audioId.toString());

        verify(audioQueryService).existsById(audioId);
        verifyNoInteractions(libraryItemRepository);
    }

    @Test
    void delete_shouldRemoveLibraryItem_whenUserAndAudioProvided() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        // when
        libraryService.delete(userId, audioId);

        // then
        verify(libraryItemRepository)
                .deleteByUserIdAndAudioId(userId, audioId);
        verifyNoMoreInteractions(libraryItemRepository);
    }


}
