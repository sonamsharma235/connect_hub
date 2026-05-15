package com.connecthub.message.client;

import com.connecthub.common.dto.RoomExistsResponse;
import com.connecthub.common.dto.RoomMembersResponse;
import com.connecthub.common.dto.RoomMembershipResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "room-service")
public interface RoomServiceClient {

    @GetMapping("/api/internal/rooms/{roomCode}/exists")
    RoomExistsResponse roomExists(@PathVariable("roomCode") String roomCode);

    @GetMapping("/api/internal/rooms/{roomCode}/membership")
    RoomMembershipResponse membership(@PathVariable("roomCode") String roomCode,
                                      @RequestParam("userEmail") String userEmail);

    @GetMapping("/api/internal/rooms/{roomCode}/members")
    RoomMembersResponse members(@PathVariable("roomCode") String roomCode);
}
