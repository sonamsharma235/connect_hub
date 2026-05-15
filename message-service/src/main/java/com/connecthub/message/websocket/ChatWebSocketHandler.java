package com.connecthub.message.websocket;

import com.connecthub.common.dto.AuthUser;
import com.connecthub.message.dto.ChatMessageResponse;
import com.connecthub.message.dto.SocketChatPayload;
import com.connecthub.message.service.ChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ObjectMapper objectMapper, ChatMessageService chatMessageService) {
        this.objectMapper = objectMapper;
        this.chatMessageService = chatMessageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomCode = (String) session.getAttributes().get(ChatHandshakeInterceptor.ROOM_CODE_ATTR);
        roomSessions.computeIfAbsent(roomCode, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        AuthUser authUser = (AuthUser) session.getAttributes().get(ChatHandshakeInterceptor.AUTH_USER_ATTR);
        String roomCode = (String) session.getAttributes().get(ChatHandshakeInterceptor.ROOM_CODE_ATTR);
        SocketChatPayload payload = objectMapper.readValue(message.getPayload(), SocketChatPayload.class);
        if (payload.content() == null || payload.content().isBlank()) {
            session.sendMessage(new TextMessage("{\"error\":\"Message content cannot be blank\"}"));
            return;
        }

        ChatMessageResponse savedMessage = chatMessageService.saveMessage(roomCode, authUser.email(), authUser.name(), payload.content());
        String outboundMessage = objectMapper.writeValueAsString(savedMessage);

        for (WebSocketSession roomSession : roomSessions.getOrDefault(roomCode, Set.of())) {
            if (roomSession.isOpen()) {
                roomSession.sendMessage(new TextMessage(outboundMessage));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomCode = (String) session.getAttributes().get(ChatHandshakeInterceptor.ROOM_CODE_ATTR);
        if (roomCode == null) {
            return;
        }
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomCode);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void notifyRoomDeleted(String roomCode) {
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "type", "ROOM_DELETED",
                    "roomCode", roomCode,
                    "message", "Room was deleted by the creator"
            ));
        } catch (Exception ex) {
            payload = "{\"type\":\"ROOM_DELETED\",\"roomCode\":\"" + roomCode + "\"}";
        }

        for (WebSocketSession roomSession : new HashSet<>(sessions)) {
            if (!roomSession.isOpen()) {
                continue;
            }
            try {
                roomSession.sendMessage(new TextMessage(payload));
            } catch (IOException ignored) {
            } finally {
                try {
                    roomSession.close(CloseStatus.NORMAL);
                } catch (IOException ignored) {
                }
            }
        }

        roomSessions.remove(roomCode);
    }
}
