package com.connecthub.notification.websocket;

import com.connecthub.common.dto.AuthUser;
import com.connecthub.notification.domain.Notification;
import com.connecthub.notification.dto.SocketNotificationPayload;
import com.connecthub.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final int socketSyncLimit;
    private final Map<String, Set<WebSocketSession>> sessionsByEmail = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper,
                                        NotificationRepository notificationRepository,
                                        @Value("${app.notifications.socket-sync-limit:25}") int socketSyncLimit) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.socketSyncLimit = Math.max(0, socketSyncLimit);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthUser authUser = (AuthUser) session.getAttributes().get(NotificationHandshakeInterceptor.AUTH_USER_ATTR);
        if (authUser == null || authUser.email() == null || authUser.email().isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing user"));
            return;
        }

        String key = authUser.email().trim().toLowerCase();
        sessionsByEmail.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(session);

        trySendSync(session, authUser.email());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AuthUser authUser = (AuthUser) session.getAttributes().get(NotificationHandshakeInterceptor.AUTH_USER_ATTR);
        if (authUser == null || authUser.email() == null) {
            return;
        }

        String key = authUser.email().trim().toLowerCase();
        Set<WebSocketSession> sessions = sessionsByEmail.get(key);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByEmail.remove(key);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void pushNotification(Notification notification) {
        if (notification == null || notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            return;
        }

        String key = notification.getRecipientEmail().trim().toLowerCase();
        Set<WebSocketSession> sessions = sessionsByEmail.get(key);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        SocketNotificationPayload payload = new SocketNotificationPayload(
                "MESSAGE",
                notification.getId(),
                notification.getRoomCode(),
                notification.getMessageId(),
                notification.getSenderEmail(),
                notification.getSenderName(),
                notification.getPreview(),
                notification.getCreatedAt()
        );

        final String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception ignored) {
            }
        }
    }

    private void trySendSync(WebSocketSession session, String recipientEmail) {
        if (socketSyncLimit <= 0 || session == null || !session.isOpen()) {
            return;
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        try {
            List<Notification> unread = notificationRepository
                    .findByRecipientEmailAndReadAtIsNullOrderByCreatedAtDesc(recipientEmail)
                    .stream()
                    .limit(socketSyncLimit)
                    .toList();

            List<SocketNotificationPayload> notifications = unread.stream()
                    .map(item -> new SocketNotificationPayload(
                            "MESSAGE",
                            item.getId(),
                            item.getRoomCode(),
                            item.getMessageId(),
                            item.getSenderEmail(),
                            item.getSenderName(),
                            item.getPreview(),
                            item.getCreatedAt()
                    ))
                    .toList();

            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "SYNC",
                    "unreadCount", notificationRepository.countByRecipientEmailAndReadAtIsNull(recipientEmail),
                    "notifications", notifications
            ));
            session.sendMessage(new TextMessage(json));
        } catch (Exception ignored) {
        }
    }
}
