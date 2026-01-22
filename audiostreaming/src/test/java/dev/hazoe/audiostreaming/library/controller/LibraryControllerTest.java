package dev.hazoe.audiostreaming.library.controller;

import dev.hazoe.audiostreaming.auth.security.JwtAuthenticationFilter;
import dev.hazoe.audiostreaming.common.security.UserPrincipal;
import dev.hazoe.audiostreaming.library.dto.LibraryItemDto;
import dev.hazoe.audiostreaming.library.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(LibraryController.class)
@AutoConfigureMockMvc(addFilters = false)
class LibraryControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LibraryService libraryService;

    @Autowired
    private ObjectMapper objectMapper;

    /* ================= LIST ================= */

    @Test
    void list_shouldReturn200_whenAuthenticated() throws Exception {
        // given
        Long userId = 1L;

        UserPrincipal principal = new UserPrincipal(
                userId,
                "FREE"
        );

        when(libraryService.list(userId))
                .thenReturn(List.of(
                        new LibraryItemDto(1L, "Audio 1", 300, false)
                ));

        // when
        var result = mvc.get()
                .uri("/api/library")
                .contentType(MediaType.APPLICATION_JSON)
                .with(request -> {
                    request.setUserPrincipal(
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
                    );
                    return request;
                })
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.OK);

        String body = result.getResponse().getContentAsString();

        LibraryItemDto[] response =
                objectMapper.readValue(body, LibraryItemDto[].class);

        assertThat(response).hasSize(1);
        assertThat(response[0].id()).isEqualTo(1L);
        assertThat(response[0].title()).isEqualTo("Audio 1");
        assertThat(response[0].durationSeconds()).isEqualTo(300);
        assertThat(response[0].premium()).isFalse();
    }

    @Test
    void save_shouldCallService_andReturn204_whenAuthenticated() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        UserPrincipal principal = new UserPrincipal(
                userId,
                "FREE"
        );

        // when
        var result = mvc.post()
                .uri("/api/library/{audioId}", audioId)
                .with(request -> {
                    request.setUserPrincipal(
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            )
                    );
                    return request;
                })
                .exchange();

        // then
        result.assertThat().hasStatus(HttpStatus.NO_CONTENT);

        verify(libraryService).save(userId, audioId);
    }
    @Test
    void delete_shouldCallService_andReturn204_whenAuthenticated() {
        // given
        Long userId = 1L;
        Long audioId = 10L;

        UserPrincipal principal = new UserPrincipal(
                userId,
                "FREE"
        );

        // when
        var result = mvc.delete()
                .uri("/api/library/{audioId}", audioId)
                .with(request -> {
                    request.setUserPrincipal(
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            )
                    );
                    return request;
                })
                .exchange();

        // then
        result.assertThat().hasStatus(HttpStatus.NO_CONTENT);

        verify(libraryService).delete(userId, audioId);
    }

}
