package com.connecthub.auth.controller;

import com.connecthub.auth.service.AuthService;
import com.connecthub.common.dto.PublicUserProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalUserController {

    private final AuthService authService;

    public InternalUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/internal/users/by-email/{email}")
    public PublicUserProfileResponse byEmail(@PathVariable String email) {
        return authService.publicProfileByEmail(email);
    }

    @GetMapping("/api/internal/users/by-username/{username}")
    public PublicUserProfileResponse byUsername(@PathVariable String username) {
        return authService.publicProfileByUsername(username);
    }
}

