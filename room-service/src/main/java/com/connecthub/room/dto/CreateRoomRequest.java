package com.connecthub.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @Size(max = 255) String description,
        @Size(max = 80) String roomCode
) {
}
