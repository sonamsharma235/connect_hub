package com.connecthub.notification.repository;

import com.connecthub.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailAndReadAtIsNullOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailAndReadAtIsNull(String recipientEmail);

    @Modifying
    @Query("""
            update Notification n
            set n.readAt = :readAt
            where n.recipientEmail = :recipientEmail
              and n.roomCode = :roomCode
              and n.readAt is null
            """)
    int markRoomRead(@Param("recipientEmail") String recipientEmail,
                     @Param("roomCode") String roomCode,
                     @Param("readAt") Instant readAt);
}

