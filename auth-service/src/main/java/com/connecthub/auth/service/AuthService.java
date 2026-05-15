package com.connecthub.auth.service;

import com.connecthub.auth.domain.AppUser;
import com.connecthub.auth.domain.AuthProvider;
import com.connecthub.auth.dto.AuthResponse;
import com.connecthub.auth.dto.LoginRequest;
import com.connecthub.auth.dto.RegisterRequest;
import com.connecthub.auth.dto.UserProfileResponse;
import com.connecthub.auth.repository.AppUserRepository;
import com.connecthub.common.dto.PublicUserProfileResponse;
import com.connecthub.common.dto.AuthUser;
import com.connecthub.common.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser user = new AppUser();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);
        user.setRole("USER");

        AppUser savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(Locale.ROOT), request.password())
        );

        AppUser user = userRepository.findByEmail(request.email().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return toAuthResponse(user);
    }

    public UserProfileResponse me(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toProfile(user);
    }

    @Transactional
    public AuthResponse updateName(String email, String name) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String normalized = normalizeName(name);
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be between 2 and 100 characters");
        }

        user.setName(normalized);
        AppUser saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    @Transactional
    public AuthResponse updateAvatar(String email, String avatarData) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String normalized = normalizeAvatar(avatarData);
        if (normalized != null && normalized.length() > 1_500_000) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Avatar is too large");
        }

        user.setAvatarData(normalized);
        AppUser saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    public UserProfileResponse profile(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        AppUser user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toProfile(user);
    }

    public UserProfileResponse profileByUsername(String username) {
        AppUser user = findByUsernameOrThrow(username);
        return toProfile(user);
    }

    public PublicUserProfileResponse publicProfileByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        AppUser user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toPublicProfile(user);
    }

    public PublicUserProfileResponse publicProfileByUsername(String username) {
        AppUser user = findByUsernameOrThrow(username);
        return toPublicProfile(user);
    }

    @Transactional
    public AppUser upsertOAuthUser(String registrationId, Map<String, Object> attributes) {
        String email = stringValue(attributes.get("email"));
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth provider did not return an email");
        }

        String name = stringValue(attributes.get("name"));
        if (name == null || name.isBlank()) {
            int separatorIndex = email.indexOf("@");
            name = separatorIndex > 0 ? email.substring(0, separatorIndex) : email;
        }

        AuthProvider provider = switch (registrationId.toLowerCase(Locale.ROOT)) {
            case "google" -> AuthProvider.GOOGLE;
            case "github" -> AuthProvider.GITHUB;
            default -> AuthProvider.LOCAL;
        };

        AppUser user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT)).orElseGet(AppUser::new);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setName(name);
        user.setProvider(provider);
        user.setRole("USER");
        if (user.getPasswordHash() == null) {
            user.setPasswordHash("");
        }
        return userRepository.save(user);
    }

    private AuthResponse toAuthResponse(AppUser user) {
        String token = jwtService.generateToken(
                new AuthUser(user.getEmail(), user.getName()),
                Map.of("role", user.getRole(), "provider", user.getProvider().name())
        );
        return new AuthResponse(token, toProfile(user));
    }

    private UserProfileResponse toProfile(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarData(),
                user.getProvider().name(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    private PublicUserProfileResponse toPublicProfile(AppUser user) {
        return new PublicUserProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }

    private AppUser findByUsernameOrThrow(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        String normalized = username.trim();
        long matches = userRepository.countByNameIgnoreCase(normalized);
        if (matches == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist");
        }
        if (matches > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple users found with this username");
        }

        return userRepository.findFirstByNameIgnoreCase(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist"));
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeAvatar(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        return trimmed;
    }
}
