package dev.hazoe.audiostreaming.audio.security;

import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("audioAuth")
@RequiredArgsConstructor
public class AudioAuthorization {

    private final AudioRepository audioRepository;

    public boolean canStream(Long id, Authentication auth) {

        return audioRepository.findById(id)
                .map(audio -> {

                    // Free audio → allow
                    if (!audio.isPremium()) {
                        return true;
                    }

                    // Premium audio → must login
                    if (auth == null ||
                            auth instanceof AnonymousAuthenticationToken ||
                            !auth.isAuthenticated()) {
                        return false;
                    }

                    // Premium role check
                    return auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(role ->
                                    role.equals("ROLE_PREMIUM")
                                            || role.equals("ROLE_ADMIN")
                            );
                })
                .orElse(true); // For service handles 404
    }
}


