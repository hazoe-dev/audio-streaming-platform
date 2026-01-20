package dev.hazoe.audiostreaming.audio.security;

import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("audioAuth")
@RequiredArgsConstructor
public class AudioAuthorization {

    private final AudioRepository audioRepository;

    public boolean canStream(Long id, Authentication auth) {
        return audioRepository.findById(id)
                .map(audio -> {
                    if (!audio.isPremium()) return true; //if audio need permission
                    return auth != null && auth.getAuthorities().stream()
                            .anyMatch(a ->
                                    a.getAuthority().equals("ROLE_PREMIUM")
                                            || a.getAuthority().equals("ROLE_ADMIN")
                            );
                })
                .orElse(true); //for service handles 404
    }
}


