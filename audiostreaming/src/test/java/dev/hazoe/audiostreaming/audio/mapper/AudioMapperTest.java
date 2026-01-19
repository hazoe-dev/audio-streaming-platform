package dev.hazoe.audiostreaming.audio.mapper;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AudioMapperTest {

    private AudioMapper audioMapper;

    @BeforeEach
    void setUp() {
        audioMapper = new AudioMapper("https://cdn.example.com");
    }

    @Test
    void toListItem_shouldMapBasicFields() {
        // given
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setTitle("Mindful Focus");
        audio.setDurationSeconds(1800);
        audio.setPremium(false);

        // when
        AudioListItemDto dto = audioMapper.toListItem(audio);

        // then
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Mindful Focus");
        assertThat(dto.durationSeconds()).isEqualTo(1800);
        assertThat(dto.isPremium()).isFalse();
    }

    @Test
    void toDetail_shouldBuildCoverUrl_whenCoverPathExists() {
        // given
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setTitle("Mindful Focus");
        audio.setDescription("Guided meditation");
        audio.setDurationSeconds(1800);
        audio.setCoverPath("covers/1.jpg");
        audio.setPremium(true);

        // when
        AudioDetailDto dto = audioMapper.toDetail(audio);

        // then
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Mindful Focus");
        assertThat(dto.description()).isEqualTo("Guided meditation");
        assertThat(dto.durationSeconds()).isEqualTo(1800);
        assertThat(dto.coverUrl())
                .isEqualTo("https://cdn.example.com/covers/1.jpg");
        assertThat(dto.isPremium()).isTrue();
    }

    @Test
    void toDetail_shouldReturnNullCoverUrl_whenCoverPathIsNull() {
        // given
        Audio audio = new Audio();
        audio.setId(1L);
        audio.setTitle("No Cover");
        audio.setDurationSeconds(1200);
        audio.setCoverPath(null);
        audio.setPremium(false);

        // when
        AudioDetailDto dto = audioMapper.toDetail(audio);

        // then
        assertThat(dto.coverUrl()).isNull();
    }
}
