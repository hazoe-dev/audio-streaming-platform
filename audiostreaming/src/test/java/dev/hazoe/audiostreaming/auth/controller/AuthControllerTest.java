package dev.hazoe.audiostreaming.auth.controller;

import dev.hazoe.audiostreaming.auth.dto.*;
import dev.hazoe.audiostreaming.auth.security.JwtAuthenticationFilter;
import dev.hazoe.audiostreaming.auth.service.AuthService;
import dev.hazoe.audiostreaming.common.exception.EmailAlreadyExistsException;
import dev.hazoe.audiostreaming.common.exception.InvalidCredentialsException;
import dev.hazoe.audiostreaming.common.exception.UnauthorizedException;
import dev.hazoe.audiostreaming.common.response.ApiErrorResponse;
import dev.hazoe.audiostreaming.common.response.ValidationErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

    /* ================= REGISTER ================= */

    @Test
    void register_shouldReturn400_whenInvalidParams() throws Exception {
        // when + then
        var result = mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {
                              "email": "user",
                              "password": "password"
                            }
                        """)
                .exchange();
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST);
        String body = result.getResponse().getContentAsString();

        ValidationErrorResponse response =
                objectMapper.readValue(
                        body,
                        ValidationErrorResponse.class);

        assertThat(response.message()).isEqualTo("Validation failed");
        verifyNoInteractions(authService);
    }

    @Test
    void register_shouldReturn409_whenExistedEmail() throws Exception {
        // given
        RegisterRequest mockRequest = new RegisterRequest(
                "test@email.com",
                "password123"
        );

        given(authService.save(mockRequest))
                .willThrow(new EmailAlreadyExistsException(mockRequest.email()));

        var result = mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest))
                .exchange();
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT);
        String body = result.getResponse().getContentAsString();

        ApiErrorResponse response =
                objectMapper.readValue(body, ApiErrorResponse.class);

        assertThat(response.error()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void register_shouldReturn201_andResponseBody() throws Exception {
        // given
        RegisterRequest mockRequest = new RegisterRequest(
                "test@email.com",
                "password123"
        );

        RegisterResponse mockResponse = new RegisterResponse(
                "test@email.com",
                "User registered successfully"
        );

        given(authService.save(mockRequest))
                .willReturn(mockResponse);

        // when + then
        var result = mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockRequest))
                .exchange();

        assertThat(result.getResponse().getStatus())
                .isEqualTo(HttpStatus.CREATED.value());

        String body = result.getResponse().getContentAsString();

        RegisterResponse response =
                objectMapper.readValue(body, RegisterResponse.class);

        assertThat(response.email()).isEqualTo("test@email.com");
        assertThat(response.message()).isEqualTo("User registered successfully");
    }

    /* ================= LOGIN ================= */
    @Test
    void login_shouldReturn400_whenInvalidParams() throws Exception {
        // when
        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "invalid-email",
                      "password": "123"
                    }
                    """)
                .exchange();

        // then
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST);

        String body = result.getResponse().getContentAsString();

        ValidationErrorResponse response =
                objectMapper.readValue(body, ValidationErrorResponse.class);

        assertThat(response.message()).isEqualTo("Validation failed");

        verifyNoInteractions(authService);
    }

    @Test
    void login_shouldReturn200_whenValidCredentials() throws Exception {
        // given
        LoginRequest request = new LoginRequest(
                "user@example.com",
                "password123"
        );

        AuthResponse authResponse = new AuthResponse(
                "access-token",
                "refresh-token"
        );

        when(authService.authenticate(any(LoginRequest.class)))
                .thenReturn(authResponse);

        // when
        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .exchange();

        // then
        assertThat(result)
                .hasStatus(HttpStatus.OK);

        String body = result.getResponse().getContentAsString();

        AuthResponse response =
                objectMapper.readValue(body, AuthResponse.class);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        verify(authService).authenticate(any(LoginRequest.class));
    }

    @Test
    void login_shouldReturn401_whenInvalidCredentials() {
        // given
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        // when
        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "user@example.com",
                      "password": "wrong-password"
                    }
                    """)
                .exchange();

        // then
        assertThat(result)
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /* ================= REFRESH ================= */
    @Test
    void refresh_shouldReturn200_whenValidRefreshToken() throws Exception {

        given(authService.refreshToken(any(RefreshTokenRequest.class)))
                .willReturn(new AuthResponse("new-access", "new-refresh"));

        var result = mvc.post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                  "refreshToken": "valid-refresh-token"
                }
            """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);

        AuthResponse response =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(),
                        AuthResponse.class
                );

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_shouldReturn401_whenInvalidRefreshToken() {

        given(authService.refreshToken(any(RefreshTokenRequest.class)))
                .willThrow(new UnauthorizedException("Invalid refresh token"));

        var result = mvc.post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                  "refreshToken": "invalid-token"
                }
            """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

}
