package com.connecthub.room.dto;

import java.time.Instant;

public record RoomResponse(
        Long id,
        String roomCode,
        String name,
        String description,
        String avatarData,
        String createdBy,
        Instant createdAt
) {
}
