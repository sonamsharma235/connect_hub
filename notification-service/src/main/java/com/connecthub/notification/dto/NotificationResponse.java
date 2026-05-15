package com.connecthub.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String roomCode,
        Long messageId,
        String senderEmail,
        String senderName,
        String preview,
        Instant createdAt
) {
}

