package com.connecthub.room.dto;

import java.time.Instant;

public record RoomMemberResponse(
        String userEmail,
        Instant joinedAt
) {
}
