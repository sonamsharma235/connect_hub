package com.connecthub.auth.service;

import com.connecthub.auth.domain.AppUser;
import com.connecthub.auth.domain.AuthProvider;
import com.connecthub.auth.dto.AuthResponse;
import com.connecthub.auth.dto.RegisterRequest;
import com.connecthub.auth.repository.AppUserRepository;
import com.connecthub.common.dto.AuthUser;
import com.connecthub.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_whenEmailAlreadyExists_throwsConflict() {
        RegisterRequest request = new RegisterRequest("John", "john@example.com", "password123");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Email already registered", ex.getReason());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void register_whenValid_savesUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("John", "JOHN@EXAMPLE.COM", "password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(jwtService.generateToken(any(AuthUser.class), anyMap())).thenReturn("token-123");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthResponse response = authService.register(request);

        assertEquals("token-123", response.token());
        assertNotNull(response.user());
        assertEquals("john@example.com", response.user().email());
        assertEquals("John", response.user().name());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());

        AppUser saved = captor.getValue();
        assertEquals("john@example.com", saved.getEmail());
        assertEquals("John", saved.getName());
        assertEquals("hash", saved.getPasswordHash());
        assertEquals(AuthProvider.LOCAL, saved.getProvider());
        assertEquals("USER", saved.getRole());
    }

    @Test
    void updateName_whenEmailMissing_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.updateName("  ", "New Name")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Email is required", ex.getReason());
    }
}
