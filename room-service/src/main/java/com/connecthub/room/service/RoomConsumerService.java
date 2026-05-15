package com.connecthub.room.service;

import com.connecthub.common.config.RabbitMQConfig;
import com.connecthub.common.dto.MessageEventDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RoomConsumerService {

    @RabbitListener(queues = RabbitMQConfig.ROOM_QUEUE)
    public void handleMessageEvent(MessageEventDTO event) {
        System.out.println(" Room service updating room: " + event.getRoomId());

        // Update room's last message & timestamp
        updateRoomLastMessage(event);
    }

    private void updateRoomLastMessage(MessageEventDTO event) {
        // roomRepository.updateLastMessage(event.getRoomId(),
        //                                  event.getContent(),
        //                                  event.getSentAt());
        System.out.println("Room " + event.getRoomId() + " last message updated.");
    }
}
