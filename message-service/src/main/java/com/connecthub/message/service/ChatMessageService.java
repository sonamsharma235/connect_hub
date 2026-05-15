package com.connecthub.message.service;

import com.connecthub.common.dto.MessageNotificationCreateRequest;
import com.connecthub.common.dto.RoomMembersResponse;
import com.connecthub.message.client.NotificationServiceClient;
import com.connecthub.message.client.RoomServiceClient;
import com.connecthub.message.domain.ChatMessage;
import com.connecthub.message.dto.ChatMessageResponse;
import com.connecthub.message.repository.ChatMessageRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final RoomServiceClient roomServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
                              RoomServiceClient roomServiceClient,
                              NotificationServiceClient notificationServiceClient) {
        this.chatMessageRepository = chatMessageRepository;
        this.roomServiceClient = roomServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    public List<ChatMessageResponse> getMessages(String roomCode, String userEmail) {
        validateRoomExists(roomCode);
        validateMembership(roomCode, userEmail);
        return chatMessageRepository.findByRoomCodeOrderBySentAtAsc(roomCode).stream()
                .map(this::toResponse)
                .toList();
    }

    public ChatMessageResponse getLatestMessage(String roomCode, String userEmail) {
        validateRoomExists(roomCode);
        validateMembership(roomCode, userEmail);
        return chatMessageRepository.findTopByRoomCodeOrderBySentAtDesc(roomCode)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public ChatMessageResponse saveMessage(String roomCode, String senderEmail, String content) {
        return saveMessage(roomCode, senderEmail, extractNameFromEmail(senderEmail), content);
    }

    @Transactional
    public ChatMessageResponse saveMessage(String roomCode, String senderEmail, String senderName, String content) {
        validateRoomExists(roomCode);
        validateMembership(roomCode, senderEmail);

        ChatMessage message = new ChatMessage();
        message.setRoomCode(roomCode);
        message.setSenderEmail(senderEmail);
        message.setSenderName(senderName == null || senderName.isBlank() ? extractNameFromEmail(senderEmail) : senderName);
        message.setContent(content);

        ChatMessageResponse saved = toResponse(chatMessageRepository.save(message));
        tryCreateNotifications(saved);
        return saved;
    }

    private void tryCreateNotifications(ChatMessageResponse saved) {
        if (saved == null || saved.roomCode() == null || saved.senderEmail() == null) {
            return;
        }

        try {
            RoomMembersResponse membersResponse = roomServiceClient.members(saved.roomCode());
            List<String> members = membersResponse == null || membersResponse.members() == null
                    ? List.of()
                    : membersResponse.members();

            if (members.isEmpty()) {
                return;
            }

            notificationServiceClient.createMessageNotifications(new MessageNotificationCreateRequest(
                    saved.roomCode(),
                    saved.id(),
                    saved.senderEmail(),
                    saved.senderName(),
                    saved.content(),
                    members
            ));
        } catch (FeignException ex) {
            log.warn("Notification-service call failed; skipping notifications. status={}", ex.status());
        } catch (Exception ex) {
            log.warn("Notification-service call failed; skipping notifications.");
        }
    }

    private void validateRoomExists(String roomCode) {
        boolean exists = roomServiceClient.roomExists(roomCode).exists();
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
    }

    private void validateMembership(String roomCode, String userEmail) {
        boolean member = roomServiceClient.membership(roomCode, userEmail).member();
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must join this room to access it");
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoomCode(),
                message.getSenderEmail(),
                message.getSenderName(),
                message.getContent(),
                message.getSentAt()
        );
    }

    private String extractNameFromEmail(String senderEmail) {
        int index = senderEmail.indexOf("@");
        return index > 0 ? senderEmail.substring(0, index) : senderEmail;
    }
}
