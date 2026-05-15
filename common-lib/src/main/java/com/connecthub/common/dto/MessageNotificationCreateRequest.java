package com.connecthub.common.dto;

import java.util.List;

public record MessageNotificationCreateRequest(
        String roomCode,
        Long messageId,
        String senderEmail,
        String senderName,
        String content,
        List<String> recipientEmails
) {
}

