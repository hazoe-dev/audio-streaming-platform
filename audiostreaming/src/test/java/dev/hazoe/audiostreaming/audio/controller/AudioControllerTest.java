package dev.hazoe.audiostreaming.audio.controller;

import dev.hazoe.audiostreaming.audio.dto.AudioDetailDto;
import dev.hazoe.audiostreaming.audio.dto.AudioListItemDto;
import dev.hazoe.audiostreaming.audio.dto.AudioStreamResponse;
import dev.hazoe.audiostreaming.audio.service.AudioService;
import dev.hazoe.audiostreaming.audio.service.AudioStreamService;
import dev.hazoe.audiostreaming.auth.security.JwtAuthenticationFilter;
import dev.hazoe.audiostreaming.common.exception.AudioNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;


@WebMvcTest(AudioController.class)
@AutoConfigureMockMvc(addFilters = false)
class AudioControllerTest {
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AudioService audioService;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private AudioStreamService streamService;

    @Test
    void getAudios_shouldReturnPagedResult() {
        AudioListItemDto dto = new AudioListItemDto(
                1L,
                "Mindful Focus",
                1800,
                false
        );

        Page<AudioListItemDto> page =
                new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        given(audioService.getAudios(any(Pageable.class)))
                .willReturn(page);

        var result = mockMvc.get()
                .uri("/api/audios")
                .param("page", "0")
                .param("size", "20")
                .exchange();
        var body = result.assertThat()
                .bodyJson();

        result.assertThat().hasStatus(HttpStatus.OK);

        body.extractingPath("$.content[0].id").isEqualTo(1);
        body.extractingPath("$.content[0].title").isEqualTo("Mindful Focus");
        body.extractingPath("$.content[0].durationSeconds").isEqualTo(1800);
        body.extractingPath("$.content[0].isPremium").isEqualTo(false);
        body.extractingPath("$.totalElements").isEqualTo(1);
        body.extractingPath("$.size").isEqualTo(20);

    }

    @Test
    void getAudio_shouldReturnAudioDetail() {
        // given
        AudioDetailDto dto = new AudioDetailDto(
                1L,
                "Mindful Focus",
                "Guided meditation",
                1800,
                "https://cdn.example.com/cover.jpg",
                false
        );

        given(audioService.getAudioDetail(1L))
                .willReturn(dto);

        // when
        var result = mockMvc.get()
                .uri("/api/audios/{id}", 1L)
                .exchange();
        // then
        var body = result.assertThat()
                .bodyJson();
        result.assertThat().hasStatus(HttpStatus.OK);

        body.extractingPath("$.id").isEqualTo(1);
        body.extractingPath("$.title").isEqualTo("Mindful Focus");
        body.extractingPath("$.description").isEqualTo("Guided meditation");
        body.extractingPath("$.durationSeconds").isEqualTo(1800);
        body.extractingPath("$.coverUrl").isEqualTo("https://cdn.example.com/cover.jpg");
        body.extractingPath("$.isPremium").isEqualTo(false);
    }

    @Test
    void getAudio_shouldReturn404_whenNotFound() {
        given(audioService.getAudioDetail(99L))
                .willThrow(new AudioNotFoundException(99L));

        var result = mockMvc.get()
                .uri("/api/audios/{id}", 99L)
                .exchange();

        result.assertThat().hasStatus(HttpStatus.NOT_FOUND);

        var body = result.assertThat().bodyJson();

        body.extractingPath("$.status").isEqualTo(404);
        body.extractingPath("$.error").isEqualTo("AUDIO_NOT_FOUND");
        body.extractingPath("$.message")
                .isEqualTo("Audio not found with id: 99");
    }

    /* ================= STREAM ================= */

    @Test
    void stream_shouldReturn200_whenAuthorizedAndExists() {
        // given

        AudioStreamResponse response = new AudioStreamResponse(
                new ByteArrayResource("test".getBytes()),
                new HttpHeaders(),
                HttpStatus.OK,
                "audio/mpeg"

        );

        given(streamService.stream(eq(1L), any()))
                .willReturn(response);

        // when
        var result = mockMvc.get()
                .uri("/api/audios/{id}/stream", 1L)
                .with(user("premium").roles("PREMIUM"))
                .exchange();

        // then
        result.assertThat().hasStatus(HttpStatus.OK);
        result.assertThat()
                .body()
                .satisfies(resource ->
                        assertThat(resource).isNotNull()
                );
    }

    @Test
    void stream_shouldReturn404_whenAudioNotFound() {
        // given

        given(streamService.stream(eq(99L), any()))
                .willThrow(new AudioNotFoundException(99L));

        // when
        var result = mockMvc.get()
                .uri("/api/audios/{id}/stream", 99L)
                .with(user("premium").roles("PREMIUM"))
                .exchange();

        // then
        result.assertThat().hasStatus(HttpStatus.NOT_FOUND);

        var body = result.assertThat().bodyJson();
        body.extractingPath("$.error").isEqualTo("AUDIO_NOT_FOUND");
        body.extractingPath("$.status").isEqualTo(404);
    }
    @Test
    void stream_shouldPassRangeHeader() {
        // given

        AudioStreamResponse response = new AudioStreamResponse(
                new ByteArrayResource("partial".getBytes()),
                new HttpHeaders(),
                HttpStatus.PARTIAL_CONTENT,
                "audio/mpeg"

        );

        given(streamService.stream(eq(1L), eq("bytes=0-100")))
                .willReturn(response);

        // when
        var result = mockMvc.get()
                .uri("/api/audios/{id}/stream", 1L)
                .header(HttpHeaders.RANGE, "bytes=0-100")
                .with(user("premium").roles("PREMIUM"))
                .exchange();

        // then
        result.assertThat().hasStatus(HttpStatus.PARTIAL_CONTENT);
    }


}
