package dev.hazoe.audiostreaming.auth.controller;

import dev.hazoe.audiostreaming.auth.dto.AuthResponse;
import dev.hazoe.audiostreaming.auth.dto.LoginRequest;
import dev.hazoe.audiostreaming.auth.service.AuthService;
import dev.hazoe.audiostreaming.auth.dto.RegisterRequest;
import dev.hazoe.audiostreaming.auth.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.save(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

}
