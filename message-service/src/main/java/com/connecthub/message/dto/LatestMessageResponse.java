package com.connecthub.message.dto;

public record LatestMessageResponse(
        String roomCode,
        ChatMessageResponse message
) {
}

