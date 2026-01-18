package dev.hazoe.audiostreaming.auth;

import dev.hazoe.audiostreaming.auth.model.Role;
import dev.hazoe.audiostreaming.auth.model.User;
import dev.hazoe.audiostreaming.auth.model.dto.RegisterRequest;
import dev.hazoe.audiostreaming.auth.model.dto.RegisterResponse;
import dev.hazoe.audiostreaming.common.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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


}