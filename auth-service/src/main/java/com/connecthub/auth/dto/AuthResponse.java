package com.connecthub.auth.dto;

public record AuthResponse(
        String token,
        UserProfileResponse user
) {
}
