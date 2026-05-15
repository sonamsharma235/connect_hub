package com.connecthub.message.service;

import com.connecthub.message.repository.ChatMessageRepository;
import com.connecthub.message.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomPurgeService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public RoomPurgeService(ChatMessageRepository chatMessageRepository, ChatWebSocketHandler chatWebSocketHandler) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Transactional
    public void purgeRoom(String roomCode) {
        chatMessageRepository.deleteByRoomCode(roomCode);
        chatWebSocketHandler.notifyRoomDeleted(roomCode);
    }
}

