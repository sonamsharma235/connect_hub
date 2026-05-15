package com.connecthub.notification.config;

import com.connecthub.notification.websocket.NotificationHandshakeInterceptor;
import com.connecthub.notification.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationHandshakeInterceptor notificationHandshakeInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                           NotificationHandshakeInterceptor notificationHandshakeInterceptor,
                           @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.notificationHandshakeInterceptor = notificationHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(notificationHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}

