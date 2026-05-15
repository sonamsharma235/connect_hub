package com.connecthub.message.controller;

import com.connecthub.message.service.RoomPurgeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalMessageController {

    private final RoomPurgeService roomPurgeService;

    public InternalMessageController(RoomPurgeService roomPurgeService) {
        this.roomPurgeService = roomPurgeService;
    }

    @DeleteMapping("/api/internal/messages/rooms/{roomCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purgeRoomMessages(@PathVariable String roomCode) {
        roomPurgeService.purgeRoom(roomCode);
    }
}

