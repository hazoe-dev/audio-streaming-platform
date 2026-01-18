package dev.hazoe.audiostreaming.auth.controller;

import dev.hazoe.audiostreaming.auth.AuthController;
import dev.hazoe.audiostreaming.auth.model.dto.RegisterRequest;
import dev.hazoe.audiostreaming.auth.model.dto.RegisterResponse;
import dev.hazoe.audiostreaming.auth.AuthService;
import dev.hazoe.audiostreaming.common.exception.EmailAlreadyExistsException;
import dev.hazoe.audiostreaming.common.exception.dto.ApiErrorResponse;
import dev.hazoe.audiostreaming.common.exception.dto.ValidationErrorResponse;
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
import static org.mockito.BDDMockito.given;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                objectMapper.readValue(body, ValidationErrorResponse.class);

        assertThat(response.message()).isEqualTo("Validation failed");
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
}
