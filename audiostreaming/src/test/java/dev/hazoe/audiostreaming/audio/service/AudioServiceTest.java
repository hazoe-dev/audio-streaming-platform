package dev.hazoe.audiostreaming.audio.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.mapper.AudioMapper;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AudioServiceTest {

    @Mock
    private AudioRepository audioRepository;

    @Mock
    private AudioMapper audioMapper;

    @InjectMocks
    private AudioService audioService;

    @Test
    void getAudios_shouldReturnMappedPage() {
        // given
        Pageable pageable = PageRequest.of(0, 20);

        Audio audio = new Audio();
        audio.setId(1L);
        audio.setTitle("Mindful Focus");
        audio.setDurationSeconds(1800);
        audio.setPremium(false);

        AudioListItemDto dto = new AudioListItemDto(
                1L,
                "Mindful Focus",
                1800,
                false
        );

        Page<Audio> audioPage =
                new PageImpl<>(List.of(audio), pageable, 1);

        given(audioRepository.findAll(pageable))
                .willReturn(audioPage);
        given(audioMapper.toListItem(audio))
                .willReturn(dto);

        // when
        Page<AudioListItemDto> result =
                audioService.getAudios(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        AudioListItemDto item = result.getContent().get(0);
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.title()).isEqualTo("Mindful Focus");
        assertThat(item.durationSeconds()).isEqualTo(1800);
        assertThat(item.isPremium()).isFalse();

        verify(audioRepository).findAll(pageable);
        verify(audioMapper).toListItem(audio);
    }

    @Test
    void getAudioDetail_shouldReturnDetail_whenAudioExists() {
        // given
        Audio audio = new Audio();
        audio.setId(1L);

        AudioDetailDto dto = new AudioDetailDto(
                1L,
                "Mindful Focus",
                "Guided meditation",
                1800,
                "https://cdn.example.com/cover.jpg",
                true
        );

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));
        given(audioMapper.toDetail(audio))
                .willReturn(dto);

        // when
        AudioDetailDto result =
                audioService.getAudioDetail(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Mindful Focus");

        verify(audioRepository).findById(1L);
        verify(audioMapper).toDetail(audio);
    }

    @Test
    void getAudioDetail_shouldThrowException_whenAudioNotFound() {
        // given
        given(audioRepository.findById(99L))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
                audioService.getAudioDetail(99L)
        )
                .isInstanceOf(AudioNotFoundException.class)
                .hasMessageContaining("99");

        verify(audioRepository).findById(99L);
    }
}
