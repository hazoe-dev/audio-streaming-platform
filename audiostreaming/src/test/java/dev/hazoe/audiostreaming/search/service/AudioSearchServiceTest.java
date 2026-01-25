package dev.hazoe.audiostreaming.search.service;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.mapper.AudioMapper;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioSearchServiceTest {

    @Mock
    private AudioRepository audioRepository;

    @Mock
    private AudioMapper audioMapper;

    @InjectMocks
    private AudioSearchService audioSearchService;

    @Test
    void search_blankKeyword_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<AudioListItemDto> result = audioSearchService.search("   ", pageable);

        assertThat(result).isEmpty();
        verifyNoInteractions(audioRepository);
        verifyNoInteractions(audioMapper);
    }

    @Test
    void search_nullKeyword_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<AudioListItemDto> result = audioSearchService.search(null, pageable);

        assertThat(result).isEmpty();
        verifyNoInteractions(audioRepository);
    }

    @Test
    void search_validKeyword_callsRepositoryWithTsQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "hello world";

        Audio audio = new Audio(); // can be minimal
        AudioListItemDto dto = new AudioListItemDto(
                10l,
                "Hello world",
                123,
                true
        );

        Page<Audio> audioPage = new PageImpl<>(List.of(audio), pageable, 1);

        when(audioRepository.search("hello:* | world:*", pageable))
                .thenReturn(audioPage);
        when(audioMapper.toListItem(audio)).thenReturn(dto);

        Page<AudioListItemDto> result =
                audioSearchService.search(keyword, pageable);

        assertThat(result.getContent()).containsExactly(dto);

        verify(audioRepository).search("hello:* | world:*", pageable);
        verify(audioMapper).toListItem(audio);
    }

    @Test
    void search_keywordWithSpecialCharacters_escapesTerms() {
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "java-spring boot!";

        when(audioRepository.search("javaspring:* | boot:*",pageable))
                .thenReturn(Page.empty(pageable));

        audioSearchService.search(keyword, pageable);

        verify(audioRepository).search("javaspring:* | boot:*", pageable);
    }

}
