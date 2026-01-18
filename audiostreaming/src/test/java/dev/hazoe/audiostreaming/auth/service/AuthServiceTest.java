package dev.hazoe.audiostreaming.auth.service;

import dev.hazoe.audiostreaming.auth.domain.Role;
import dev.hazoe.audiostreaming.auth.domain.User;
import dev.hazoe.audiostreaming.auth.dto.AuthResponse;
import dev.hazoe.audiostreaming.auth.dto.LoginRequest;
import dev.hazoe.audiostreaming.auth.dto.RegisterRequest;
import dev.hazoe.audiostreaming.auth.dto.RegisterResponse;
import dev.hazoe.audiostreaming.auth.repository.UserRepository;
import dev.hazoe.audiostreaming.auth.security.JwtProvider;
import dev.hazoe.audiostreaming.common.exception.EmailAlreadyExistsException;
import dev.hazoe.audiostreaming.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;


    @Test
    void shouldThrowException_whenEmailAlreadyExists() {
        // given
        RegisterRequest request = new RegisterRequest(
                "test@email.com",
                "password123"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        // when + then
        assertThatThrownBy(() -> authService.save(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(request.email());

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // given
        RegisterRequest request = new RegisterRequest(
                "test@email.com",
                "password123"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("hashed-password");

        // when
        RegisterResponse response = authService.save(request);

        // then
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.message()).isEqualTo("User registered successfully");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldSaveUserWithCorrectFields() {
        // given
        RegisterRequest request = new RegisterRequest(
                "test@email.com",
                "password123"
        );

        when(userRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-password");

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        // when
        authService.save(request);

        // then
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo(request.email());
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.FREE);
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldNotStorePlainPassword() {
        // given
        RegisterRequest request = new RegisterRequest(
                "test@email.com",
                "password123"
        );

        when(userRepository.existsByEmail(any()))
                .thenReturn(false);

        when(passwordEncoder.encode(any()))
                .thenReturn("hashed-password");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // when
        authService.save(request);

        // then
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPasswordHash())
                .isNotEqualTo(request.password());
    }

    @Test
    void authenticate_success_shouldReturnToken() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.FREE);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .thenReturn(true);

        when(jwtProvider.generateToken(1L, "FREE"))
                .thenReturn("jwt-token");

        // when
        AuthResponse response = authService.authenticate(request);

        // then
        assertNotNull(response);
        assertEquals("jwt-token", response.accessToken());

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "hashed-password");
        verify(jwtProvider).generateToken(1L, "FREE");
    }

    @Test
    void authenticate_emailNotFound_shouldThrowInvalidCredentials() {
        // given
        LoginRequest request = new LoginRequest("notfound@example.com", "password123");

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        // when + then
        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        verify(userRepository).findByEmail("notfound@example.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void authenticate_wrongPassword_shouldThrowInvalidCredentials() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.FREE);

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .thenReturn(false);

        // when + then
        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        verify(passwordEncoder).matches("wrong-password", "hashed-password");
        verifyNoInteractions(jwtProvider);
    }

}