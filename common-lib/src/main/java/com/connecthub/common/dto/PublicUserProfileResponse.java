package com.connecthub.common.dto;

import java.time.Instant;

public record PublicUserProfileResponse(
        String name,
        String email,
        Instant createdAt
) {
}

