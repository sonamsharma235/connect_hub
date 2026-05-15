package com.connecthub.room.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomMemberChangeRequest(
        @NotBlank @Email @Size(max = 150) String memberEmail
) {
}

