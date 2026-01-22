package dev.hazoe.audiostreaming.auth.security;

import dev.hazoe.audiostreaming.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenValidBearerToken() throws Exception {
        // given
        String token = "valid-token";

        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getAuthorities()).thenReturn(List.of());

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtProvider.getPrincipalFromToken(token))
                .thenReturn(principal);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(principal);
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenNoAuthorizationHeader() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldClearContext_whenTokenInvalid() throws Exception {
        // given
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer invalid-token");

        when(jwtProvider.getPrincipalFromToken("invalid-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotOverrideExistingAuthentication() throws Exception {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                mock(UsernamePasswordAuthenticationToken.class)
        );

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        verifyNoInteractions(jwtProvider);
        verify(filterChain).doFilter(request, response);
    }

}
