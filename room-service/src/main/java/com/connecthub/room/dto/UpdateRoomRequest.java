package com.connecthub.room.dto;

import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
        @Size(min = 2, max = 120) String name,
        @Size(max = 255) String description,
        String avatarData
) {
}
