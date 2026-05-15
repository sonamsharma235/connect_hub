package com.connecthub.notification.service;

import com.connecthub.common.dto.MessageNotificationCreateRequest;
import com.connecthub.notification.domain.Notification;
import com.connecthub.notification.dto.NotificationResponse;
import com.connecthub.notification.repository.NotificationRepository;
import com.connecthub.notification.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final int previewMaxLength;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationWebSocketHandler notificationWebSocketHandler,
                               @Value("${app.notifications.preview-max-length:120}") int previewMaxLength) {
        this.notificationRepository = notificationRepository;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.previewMaxLength = Math.max(20, previewMaxLength);
    }

    public List<NotificationResponse> unreadForUser(String recipientEmail) {
        return notificationRepository.findByRecipientEmailAndReadAtIsNullOrderByCreatedAtDesc(recipientEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    public long unreadCountForUser(String recipientEmail) {
        return notificationRepository.countByRecipientEmailAndReadAtIsNull(recipientEmail);
    }

    @Transactional
    public int markRoomRead(String recipientEmail, String roomCode) {
        return notificationRepository.markRoomRead(recipientEmail, roomCode, Instant.now());
    }

    @Transactional
    public int createMessageNotifications(MessageNotificationCreateRequest request) {
        if (request == null) {
            return 0;
        }
        if (isBlank(request.roomCode()) || isBlank(request.senderEmail()) || request.recipientEmails() == null) {
            return 0;
        }

        List<Notification> notifications = request.recipientEmails().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .filter(email -> !email.equalsIgnoreCase(request.senderEmail()))
                .distinct()
                .map(recipient -> {
                    Notification notification = new Notification();
                    notification.setRecipientEmail(recipient);
                    notification.setRoomCode(request.roomCode().trim());
                    notification.setMessageId(request.messageId());
                    notification.setSenderEmail(request.senderEmail().trim());
                    notification.setSenderName(request.senderName());
                    notification.setPreview(buildPreview(request.content()));
                    return notification;
                })
                .toList();

        if (notifications.isEmpty()) {
            return 0;
        }

        List<Notification> saved = notificationRepository.saveAll(notifications);
        for (Notification notification : saved) {
            notificationWebSocketHandler.pushNotification(notification);
        }
        return saved.size();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRoomCode(),
                notification.getMessageId(),
                notification.getSenderEmail(),
                notification.getSenderName(),
                notification.getPreview(),
                notification.getCreatedAt()
        );
    }

    private String buildPreview(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            normalized = "New message";
        }
        if (normalized.length() <= previewMaxLength) {
            return normalized;
        }
        return normalized.substring(0, previewMaxLength - 3) + "...";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}
