package com.connecthub.message.dto;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        String roomCode,
        String senderEmail,
        String senderName,
        String content,
        Instant sentAt
) {
}
