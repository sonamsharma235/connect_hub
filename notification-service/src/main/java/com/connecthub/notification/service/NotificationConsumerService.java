package com.connecthub.notification.service;

import com.connecthub.common.config.RabbitMQConfig;
import com.connecthub.common.dto.MessageEventDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumerService {

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleMessageEvent(MessageEventDTO event) {
        System.out.println("🔔 Notification received for user: " + event.getReceiverId());

        // Your notification logic here:
        // 1. Check if receiver is online (call room-service or cache)
        // 2. If offline → send push notification (FCM/Firebase)
        // 3. If online  → send via WebSocket

        sendNotification(event);
    }

    private void sendNotification(MessageEventDTO event) {
        System.out.println("Sending notification to: " + event.getReceiverId()
                + " | Message: " + event.getContent());
        // TODO: integrate Firebase / WebSocket
    }
}