package dev.hazoe.audiostreaming.progress.controller;

import dev.hazoe.audiostreaming.auth.security.JwtAuthenticationFilter;
import dev.hazoe.audiostreaming.common.security.UserPrincipal;
import dev.hazoe.audiostreaming.progress.service.ListeningProgressService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.mockito.Mockito.*;

@WebMvcTest(ListeningProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
class ListeningProgressControllerTest {
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvcTester mockMvc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    @MockitoBean
    private ListeningProgressService progressService;

    @Test
    void saveProgress_whenValidRequest_shouldReturn204() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(1L);
        when(principal.getAuthorities()).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        String requestBody = """
                {
                  "audioId": 10,
                  "positionSeconds": 120
                }
                """;

        mockMvc.put()
                .uri("/api/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.NO_CONTENT);

        verify(progressService).saveProgress(1L, 10L, 120);
    }

    @Test
    void saveProgress_whenInvalidRequest_shouldReturn400() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(1L);
        when(principal.getAuthorities()).thenReturn(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );

        String requestBody = """
                {
                  "audioId": 10,
                  "positionSeconds": -5
                }
                """;

        mockMvc.put()
                .uri("/api/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(progressService);
    }
}
