package com.connecthub.room.service;

import com.connecthub.room.domain.Room;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, roomMemberRepository, null, null);
    }

    @Test
    void createRoom_whenNameAlreadyExists_throwsConflict() {
        CreateRoomRequest request = new CreateRoomRequest("My Room", "desc", "");

        when(roomRepository.existsByNameIgnoreCase("My Room")).thenReturn(true);
        when(roomRepository.existsByRoomCode(anyString())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.createRoom(request, "me@example.com")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Room name already exists", ex.getReason());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void createRoom_whenCustomRoomCodeIsInvalid_throwsBadRequest() {
        CreateRoomRequest request = new CreateRoomRequest("Room", "desc", "!!!");

        when(roomRepository.existsByNameIgnoreCase("Room")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.createRoom(request, "me@example.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Room id is invalid", ex.getReason());
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void getRoomForUser_whenNotMember_throwsForbidden() {
        Room room = new Room();
        room.setRoomCode("room-1");
        room.setName("Room 1");
        room.setCreatedBy("owner@example.com");

        when(roomRepository.findByRoomCode("room-1")).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomAndUserEmail(room, "me@example.com")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.getRoomForUser("room-1", "me@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("You must join this room to access it", ex.getReason());
    }
}
