package com.connecthub.message.service;

import com.connecthub.common.config.RabbitMQConfig;
import com.connecthub.common.dto.MessageEventDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducerService {

    private final RabbitTemplate rabbitTemplate;

    public MessageProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishMessageEvent(MessageEventDTO event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.CHAT_EXCHANGE,
            RabbitMQConfig.MESSAGE_ROUTING_KEY,
            event
        );
        System.out.println(" Published to RabbitMQ: " + event.getMessageId());
    }
}