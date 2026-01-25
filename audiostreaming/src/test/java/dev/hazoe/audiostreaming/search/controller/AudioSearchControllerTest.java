package dev.hazoe.audiostreaming.search.controller;

import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.auth.security.JwtAuthenticationFilter;
import dev.hazoe.audiostreaming.search.service.AudioSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@WebMvcTest(AudioSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class AudioSearchControllerTest {

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AudioSearchService searchService;

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    void searchAudios_shouldReturnPagedResult() {
        // given
        AudioListItemDto dto = new AudioListItemDto(
                1L,
                "Mindful Focus",
                1800,
                false
        );

        Page<AudioListItemDto> page =
                new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        given(searchService.search(eq("mindful"), any(Pageable.class)))
                .willReturn(page);

        // when
        var result = mockMvc.get()
                .uri("/api/audios/search")
                .param("q", "mindful")
                .param("page", "0")
                .param("size", "20")
                .exchange();

        // then
        var body = result.assertThat().bodyJson();

        result.assertThat().hasStatus(HttpStatus.OK);

        body.extractingPath("$.content[0].id").isEqualTo(1);
        body.extractingPath("$.content[0].title").isEqualTo("Mindful Focus");
        body.extractingPath("$.content[0].durationSeconds").isEqualTo(1800);
        body.extractingPath("$.content[0].isPremium").isEqualTo(false);
        body.extractingPath("$.totalElements").isEqualTo(1);
        body.extractingPath("$.size").isEqualTo(20);
    }
}
