package dev.hazoe.audiostreaming.auth;

import dev.hazoe.audiostreaming.auth.model.Role;
import dev.hazoe.audiostreaming.auth.model.User;
import dev.hazoe.audiostreaming.auth.model.dto.RegisterRequest;
import dev.hazoe.audiostreaming.auth.model.dto.RegisterResponse;
import dev.hazoe.audiostreaming.common.exception.EmailAlreadyExistsException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
