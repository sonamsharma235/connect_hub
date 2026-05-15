package com.connecthub.auth.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String avatarData,
        String provider,
        String role,
        Instant createdAt
) {
}
