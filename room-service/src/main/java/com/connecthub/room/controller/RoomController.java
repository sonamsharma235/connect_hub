package com.connecthub.room.controller;

import com.connecthub.common.dto.RoomExistsResponse;
import com.connecthub.common.dto.RoomMembersResponse;
import com.connecthub.common.dto.RoomMembershipResponse;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.RoomMemberChangeRequest;
import com.connecthub.room.dto.RoomMemberResponse;
import com.connecthub.room.dto.RoomResponse;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/api/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request,
                                   @AuthenticationPrincipal User principal) {
        return roomService.createRoom(request, principal.getUsername());
    }

    @GetMapping("/api/rooms")
    public List<RoomResponse> listRooms(@AuthenticationPrincipal User principal) {
        return roomService.listRoomsForUser(principal.getUsername());
    }

    @GetMapping("/api/rooms/{roomCode}")
    public RoomResponse getRoom(@PathVariable String roomCode,
                                @AuthenticationPrincipal User principal) {
        return roomService.getRoomForUser(roomCode, principal.getUsername());
    }

    @PostMapping("/api/rooms/{roomCode}/join")
    public RoomResponse joinRoom(@PathVariable String roomCode,
                                 @AuthenticationPrincipal User principal) {
        return roomService.joinRoom(roomCode, principal.getUsername());
    }

    @PostMapping("/api/rooms/dm/{username}")
    public RoomResponse directMessage(@PathVariable String username,
                                      @AuthenticationPrincipal User principal) {
        return roomService.createOrGetDirectMessageRoom(username, principal.getUsername());
    }

    @DeleteMapping("/api/rooms/{roomCode}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveRoom(@PathVariable String roomCode,
                          @AuthenticationPrincipal User principal) {
        roomService.leaveRoom(roomCode, principal.getUsername());
    }

    @PatchMapping("/api/rooms/{roomCode}")
    public RoomResponse updateRoom(@PathVariable String roomCode,
                                   @Valid @RequestBody UpdateRoomRequest request,
                                   @AuthenticationPrincipal User principal) {
        return roomService.updateRoom(roomCode, request, principal.getUsername());
    }

    @DeleteMapping("/api/rooms/{roomCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable String roomCode,
                           @AuthenticationPrincipal User principal) {
        roomService.deleteRoom(roomCode, principal.getUsername());
    }

    @GetMapping("/api/rooms/{roomCode}/members")
    public List<RoomMemberResponse> members(@PathVariable String roomCode,
                                            @AuthenticationPrincipal User principal) {
        return roomService.getMembersForUser(roomCode, principal.getUsername());
    }

    @PostMapping("/api/rooms/{roomCode}/members")
    public List<RoomMemberResponse> addMember(@PathVariable String roomCode,
                                              @Valid @RequestBody RoomMemberChangeRequest request,
                                              @AuthenticationPrincipal User principal) {
        return roomService.addMember(roomCode, principal.getUsername(), request.memberEmail());
    }

    @DeleteMapping("/api/rooms/{roomCode}/members")
    public List<RoomMemberResponse> removeMember(@PathVariable String roomCode,
                                                 @RequestParam("memberEmail") String memberEmail,
                                                 @AuthenticationPrincipal User principal) {
        return roomService.removeMember(roomCode, principal.getUsername(), memberEmail);
    }

    @GetMapping("/api/internal/rooms/{roomCode}/exists")
    public RoomExistsResponse roomExists(@PathVariable String roomCode) {
        return new RoomExistsResponse(roomService.roomExists(roomCode));
    }

    @GetMapping("/api/internal/rooms/{roomCode}/membership")
    public RoomMembershipResponse membership(@PathVariable String roomCode,
                                             @RequestParam("userEmail") String userEmail) {
        return new RoomMembershipResponse(roomService.isMember(roomCode, userEmail));
    }

    @GetMapping("/api/internal/rooms/{roomCode}/members")
    public RoomMembersResponse internalMembers(@PathVariable String roomCode) {
        return new RoomMembersResponse(roomService.getMemberEmails(roomCode));
    }
}
