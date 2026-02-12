package dev.hazoe.audiostreaming.search.service;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.service.expose.AudioQueryService;
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
    private AudioQueryService audioQueryService;

    @InjectMocks
    private AudioSearchService audioSearchService;

    @Test
    void search_blankKeyword_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<AudioListItemDto> result = audioSearchService.search("   ", pageable);

        assertThat(result).isEmpty();
        verifyNoInteractions(audioQueryService);
    }

    @Test
    void search_nullKeyword_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<AudioListItemDto> result = audioSearchService.search(null, pageable);

        assertThat(result).isEmpty();
        verifyNoInteractions(audioQueryService);
    }

    @Test
    void search_validKeyword_callsRepositoryWithTsQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "hello world";

        AudioListItemDto audio = new AudioListItemDto(
                10l,
                "Hello world",
                123,
                true
        );

        Page<AudioListItemDto> audioPage = new PageImpl<>(List.of(audio), pageable, 1);

        when(audioQueryService.search("hello:* | world:*", pageable))
                .thenReturn(audioPage);

        Page<AudioListItemDto> result =
                audioSearchService.search(keyword, pageable);

        assertThat(result.getContent()).containsExactly(audio);

        verify(audioQueryService).search("hello:* | world:*", pageable);
    }

    @Test
    void search_keywordWithSpecialCharacters_escapesTerms() {
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "java-spring boot!";

        when(audioQueryService.search("javaspring:* | boot:*",pageable))
                .thenReturn(Page.empty(pageable));

        audioSearchService.search(keyword, pageable);

        verify(audioQueryService).search("javaspring:* | boot:*", pageable);
    }

}
