package com.connecthub.message.service;

import com.connecthub.common.dto.MessageEventDTO;        // ✅ ADD THIS
import org.springframework.stereotype.Service;
@Service
public class MessageService {

    private final MessageProducerService producerService;
    // private final MessageRepository messageRepository; // your existing repo

    public MessageService(MessageProducerService producerService) {
        this.producerService = producerService;
    }

    public void sendMessage(MessageEventDTO dto) {
        // 1. Save message to DB (your existing logic)
        // messageRepository.save(mapToEntity(dto));

        // 2. Publish event to RabbitMQ → notifies other services
        producerService.publishMessageEvent(dto);
    }
}