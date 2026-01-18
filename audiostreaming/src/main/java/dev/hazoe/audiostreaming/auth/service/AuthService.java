package dev.hazoe.audiostreaming.auth.service;

import dev.hazoe.audiostreaming.auth.dto.AuthResponse;
import dev.hazoe.audiostreaming.auth.dto.LoginRequest;
import dev.hazoe.audiostreaming.auth.repository.UserRepository;
import dev.hazoe.audiostreaming.auth.domain.Role;
import dev.hazoe.audiostreaming.auth.domain.User;
import dev.hazoe.audiostreaming.auth.dto.RegisterRequest;
import dev.hazoe.audiostreaming.auth.dto.RegisterResponse;
import dev.hazoe.audiostreaming.auth.security.JwtProvider;
import dev.hazoe.audiostreaming.common.exception.EmailAlreadyExistsException;
import dev.hazoe.audiostreaming.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public RegisterResponse save(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User newUser = new User();
        newUser.setEmail(request.email());
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setRole(Role.FREE);
        newUser.setCreatedAt(Instant.now());

        userRepository.save(newUser);

        return new RegisterResponse(
                newUser.getEmail(),
                "User registered successfully"
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtProvider.generateToken(
                user.getId(),
                user.getRole().name()
        );

        return new AuthResponse(token);
    }
}
