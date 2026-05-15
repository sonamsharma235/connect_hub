package com.connecthub.auth.controller;

import com.connecthub.auth.dto.AuthResponse;
import com.connecthub.auth.dto.LoginRequest;
import com.connecthub.auth.dto.RegisterRequest;
import com.connecthub.auth.dto.UpdateAvatarRequest;
import com.connecthub.auth.dto.UpdateProfileRequest;
import com.connecthub.auth.dto.UserProfileResponse;
import com.connecthub.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        return authService.me(principal.getUsername());
    }

    @PatchMapping("/me")
    public AuthResponse updateMe(@Valid @RequestBody UpdateProfileRequest request,
                                 @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        return authService.updateName(principal.getUsername(), request.name());
    }

    @PatchMapping("/me/avatar")
    public AuthResponse updateAvatar(@RequestBody UpdateAvatarRequest request,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        return authService.updateAvatar(principal.getUsername(), request == null ? null : request.avatarData());
    }

    @GetMapping("/users/{email}")
    public UserProfileResponse userProfile(@PathVariable String email) {
        return authService.profile(email);
    }

    @GetMapping("/users/by-username/{username}")
    public UserProfileResponse userProfileByUsername(@PathVariable String username) {
        return authService.profileByUsername(username);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}
