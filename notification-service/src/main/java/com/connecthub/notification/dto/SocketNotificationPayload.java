package com.connecthub.notification.dto;

import java.time.Instant;

public record SocketNotificationPayload(
        String type,
        Long id,
        String roomCode,
        Long messageId,
        String senderEmail,
        String senderName,
        String preview,
        Instant createdAt
) {
}

