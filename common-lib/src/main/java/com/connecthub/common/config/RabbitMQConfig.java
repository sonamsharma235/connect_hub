package com.connecthub.common.config;

public final class RabbitMQConfig {

    private RabbitMQConfig() {}

    // Exchange
    public static final String CHAT_EXCHANGE = "connecthub.chat.exchange";

    // Queues
    public static final String NOTIFICATION_QUEUE = "connecthub.notification.queue";
    public static final String ROOM_QUEUE = "connecthub.room.queue";

    // Routing Keys
    public static final String MESSAGE_ROUTING_KEY = "chat.message.sent";
    public static final String ROOM_ROUTING_KEY = "chat.room.updated";
}
