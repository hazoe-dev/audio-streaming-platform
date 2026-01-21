package dev.hazoe.audiostreaming.audio.security;

import dev.hazoe.audiostreaming.audio.domain.Audio;
import dev.hazoe.audiostreaming.audio.repository.AudioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AudioAuthorizationTest {

    @Mock
    private AudioRepository audioRepository;

    @InjectMocks
    private AudioAuthorization audioAuthorization;

    @Test
    void canStream_shouldReturnTrue_whenAudioIsNotPremium() {
        // given
        Audio audio = new Audio();
        audio.setPremium(false);

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        boolean result = audioAuthorization.canStream(1L, null);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void canStream_shouldReturnFalse_whenAudioIsPremium_andAuthIsNull() {
        // given
        Audio audio = new Audio();
        audio.setPremium(true);

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        boolean result = audioAuthorization.canStream(1L, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void canStream_shouldReturnFalse_whenAudioIsPremium_andUserHasNoPremiumRole() {
        // given
        Audio audio = new Audio();
        audio.setPremium(true);

        Authentication auth = new TestingAuthenticationToken(
                "user",
                null,
                "ROLE_USER"
        );

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        boolean result = audioAuthorization.canStream(1L, auth);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void canStream_shouldReturnTrue_whenAudioIsPremium_andUserHasPremiumRole() {
        // given
        Audio audio = new Audio();
        audio.setPremium(true);

        Authentication auth = new TestingAuthenticationToken(
                "premiumUser",
                null,
                new SimpleGrantedAuthority("ROLE_PREMIUM")
        );

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        boolean result = audioAuthorization.canStream(1L, auth);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void canStream_shouldReturnTrue_whenAudioIsPremium_andUserIsAdmin() {
        // given
        Audio audio = new Audio();
        audio.setPremium(true);

        Authentication auth = new TestingAuthenticationToken(
                "admin",
                null,
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        given(audioRepository.findById(1L))
                .willReturn(Optional.of(audio));

        // when
        boolean result = audioAuthorization.canStream(1L, auth);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void canStream_shouldReturnTrue_whenAudioNotFound() {
        // given
        given(audioRepository.findById(1L))
                .willReturn(Optional.empty());

        // when
        boolean result = audioAuthorization.canStream(1L, null);

        // then
        assertThat(result).isTrue();
    }
}