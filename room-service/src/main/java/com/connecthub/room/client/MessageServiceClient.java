package com.connecthub.room.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "message-service")
public interface MessageServiceClient {

    @DeleteMapping("/api/internal/messages/rooms/{roomCode}")
    void purgeRoomMessages(@PathVariable("roomCode") String roomCode);
}

