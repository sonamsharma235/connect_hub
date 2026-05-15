package com.connecthub.message.websocket;

import com.connecthub.common.dto.AuthUser;
import com.connecthub.common.security.JwtService;
import com.connecthub.message.client.RoomServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ROOM_CODE_ATTR = "roomCode";
    public static final String AUTH_USER_ATTR = "authUser";

    private final JwtService jwtService;
    private final RoomServiceClient roomServiceClient;

    public ChatHandshakeInterceptor(JwtService jwtService, RoomServiceClient roomServiceClient) {
        this.jwtService = jwtService;
        this.roomServiceClient = roomServiceClient;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        String roomCode = servletRequest.getServletRequest().getParameter("roomCode");

        if (token == null || token.isBlank() || roomCode == null || roomCode.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            AuthUser authUser = jwtService.parseUser(token);
            if (!roomServiceClient.roomExists(roomCode).exists()) {
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }
            if (!roomServiceClient.membership(roomCode, authUser.email()).member()) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            attributes.put(ROOM_CODE_ATTR, roomCode);
            attributes.put(AUTH_USER_ATTR, authUser);
            return true;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
