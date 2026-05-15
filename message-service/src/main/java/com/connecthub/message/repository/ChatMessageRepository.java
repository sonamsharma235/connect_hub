package com.connecthub.message.repository;

import com.connecthub.message.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomCodeOrderBySentAtAsc(String roomCode);

    Optional<ChatMessage> findTopByRoomCodeOrderBySentAtDesc(String roomCode);

    long deleteByRoomCode(String roomCode);
}
