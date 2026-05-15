package com.connecthub.room.service;

import com.connecthub.common.dto.PublicUserProfileResponse;
import com.connecthub.room.client.AuthServiceClient;
import com.connecthub.room.client.MessageServiceClient;
import com.connecthub.room.domain.Room;
import com.connecthub.room.domain.RoomMember;
import com.connecthub.room.dto.CreateRoomRequest;
import com.connecthub.room.dto.RoomMemberResponse;
import com.connecthub.room.dto.RoomResponse;
import com.connecthub.room.dto.UpdateRoomRequest;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageServiceClient messageServiceClient;
    private final AuthServiceClient authServiceClient;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       MessageServiceClient messageServiceClient,
                       AuthServiceClient authServiceClient) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageServiceClient = messageServiceClient;
        this.authServiceClient = authServiceClient;
    }
    
    // If anything fails midway, ALL DB changes are rolled back
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, String userEmail) {
        String roomName = normalizeRoomName(request.name());
        boolean nameExists = roomRepository.existsByNameIgnoreCase(roomName);

        String roomCode;
        boolean codeExists = false;
        if (request.roomCode() == null || request.roomCode().isBlank()) {
            roomCode = generateRoomCode(roomName);
        } else {
            roomCode = sanitize(request.roomCode());
            if (roomCode.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room id is invalid");
            }
            codeExists = roomRepository.existsByRoomCode(roomCode);
        }

        if (codeExists && nameExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room id already exists and room name already exists");
        }
        if (codeExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room id already exists");
        }
        if (nameExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room name already exists");
        }

        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setName(roomName);
        room.setDescription(request.description());
        room.setCreatedBy(userEmail);

        Room savedRoom = roomRepository.save(room);
        addMemberIfMissing(savedRoom, userEmail);
        return toResponse(savedRoom);
    }

    public List<RoomResponse> listRoomsForUser(String userEmail) {
        return roomMemberRepository.findRoomsForUser(userEmail).stream()
                .map(room -> toResponseForUser(room, userEmail))
                .toList();
    }

    public RoomResponse getRoomForUser(String roomCode, String userEmail) {
        Room room = findRoomOrThrow(roomCode);
        ensureMember(room, userEmail);
        return toResponseForUser(room, userEmail);
    }

    @Transactional
    public RoomResponse joinRoom(String roomCode, String userEmail) {
        Room room = findRoomOrThrow(roomCode);
        addMemberIfMissing(room, userEmail);
        return toResponseForUser(room, userEmail);
    }

    @Transactional
    public void leaveRoom(String roomCode, String userEmail) {
        Room room = findRoomOrThrow(roomCode);
        roomMemberRepository.deleteByRoomAndUserEmail(room, userEmail);
    }

    @Transactional
    public RoomResponse createOrGetDirectMessageRoom(String username, String userEmail) {
        PublicUserProfileResponse otherUser;
        try {
            otherUser = authServiceClient.byUsername(username);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist");
        } catch (FeignException.Conflict ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple users found with this username");
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to verify user right now");
        }

        if (otherUser == null || otherUser.email() == null || otherUser.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist");
        }

        String otherEmail = otherUser.email().toLowerCase(Locale.ROOT);
        if (otherEmail.equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot DM yourself");
        }

        String dmRoomCode = dmRoomCode(userEmail, otherEmail);
        Room room = roomRepository.findByRoomCode(dmRoomCode).orElseGet(() -> {
            Room created = new Room();
            created.setRoomCode(dmRoomCode);
            created.setName("Direct message");
            created.setDescription(null);
            created.setCreatedBy(userEmail);
            return roomRepository.save(created);
        });

        addMemberIfMissing(room, userEmail);
        addMemberIfMissing(room, otherEmail);

        return toResponseForUser(room, userEmail);
    }

    @Transactional
    public RoomResponse updateRoom(String roomCode, UpdateRoomRequest request, String userEmail) {
        Room room = findRoomOrThrow(roomCode);

        if (room.getCreatedBy() == null || !room.getCreatedBy().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room creator can update this room");
        }

        if (request == null) {
            return toResponse(room);
        }

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name cannot be blank");
            }
            String normalizedName = normalizeRoomName(request.name());
            if (roomRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, room.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Room name already exists");
            }
            room.setName(normalizedName);
        }

        if (request.description() != null) {
            String normalizedDescription = request.description().trim();
            room.setDescription(normalizedDescription.isBlank() ? null : normalizedDescription);
        }

        if (request.avatarData() != null) {
            String normalized = normalizeAvatar(request.avatarData());
            if (normalized != null && normalized.length() > 1_500_000) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Avatar is too large");
            }
            room.setAvatarData(normalized);
        }

        return toResponse(room);
    }

    public List<RoomMemberResponse> getMembersForUser(String roomCode, String userEmail) {
        Room room = findRoomOrThrow(roomCode);
        ensureMember(room, userEmail);
        return roomMemberRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(member -> new RoomMemberResponse(member.getUserEmail(), member.getJoinedAt()))
                .toList();
    }

    public List<String> getMemberEmails(String roomCode) {
        Room room = findRoomOrThrow(roomCode);
        return roomMemberRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(RoomMember::getUserEmail)
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean roomExists(String roomCode) {
        return roomRepository.existsByRoomCode(roomCode);
    }

    public boolean isMember(String roomCode, String userEmail) {
        Room room = findRoomOrThrow(roomCode);
        return roomMemberRepository.existsByRoomAndUserEmail(room, userEmail);
    }

    @Transactional
    public List<RoomMemberResponse> addMember(String roomCode, String userEmail, String memberEmail) {
        Room room = findRoomOrThrow(roomCode);
        ensureMember(room, userEmail);
        ensureCreator(room, userEmail);
        ensureMemberManagementAllowed(room);

        String targetEmail = normalizeEmail(memberEmail);
        if (targetEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member email is required");
        }

        if (userEmail != null && userEmail.equalsIgnoreCase(targetEmail)) {
            return roomMemberRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                    .map(member -> new RoomMemberResponse(member.getUserEmail(), member.getJoinedAt()))
                    .toList();
        }

        ensureUserExistsByEmail(targetEmail);
        addMemberIfMissing(room, targetEmail);

        return roomMemberRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(member -> new RoomMemberResponse(member.getUserEmail(), member.getJoinedAt()))
                .toList();
    }

    @Transactional
    public List<RoomMemberResponse> removeMember(String roomCode, String userEmail, String memberEmail) {
        Room room = findRoomOrThrow(roomCode);
        ensureMember(room, userEmail);
        ensureCreator(room, userEmail);
        ensureMemberManagementAllowed(room);

        String targetEmail = normalizeEmail(memberEmail);
        if (targetEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member email is required");
        }

        if (room.getCreatedBy() != null && room.getCreatedBy().equalsIgnoreCase(targetEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot remove the room creator");
        }

        if (userEmail != null && userEmail.equalsIgnoreCase(targetEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use leave room to remove yourself");
        }

        long deleted = roomMemberRepository.deleteByRoomAndUserEmail(room, targetEmail);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in this room");
        }

        return roomMemberRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(member -> new RoomMemberResponse(member.getUserEmail(), member.getJoinedAt()))
                .toList();
    }

    @Transactional
    public void deleteRoom(String roomCode, String userEmail) {
        Room room = findRoomOrThrow(roomCode);

        if (room.getCreatedBy() == null || !room.getCreatedBy().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room creator can delete this room");
        }

        try {
            messageServiceClient.purgeRoomMessages(roomCode);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to delete room messages right now");
        }

        roomMemberRepository.deleteByRoom(room);
        roomRepository.delete(room);
    }

    private Room findRoomOrThrow(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private void ensureMember(Room room, String userEmail) {
        if (!roomMemberRepository.existsByRoomAndUserEmail(room, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must join this room to access it");
        }
    }

    private void ensureCreator(Room room, String userEmail) {
        if (room.getCreatedBy() == null || userEmail == null || !room.getCreatedBy().equalsIgnoreCase(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room creator can manage members");
        }
    }

    private void ensureMemberManagementAllowed(Room room) {
        if (room == null) {
            return;
        }
        String code = room.getRoomCode();
        if (code != null && code.startsWith("dm-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direct messages do not support managing members");
        }
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureUserExistsByEmail(String email) {
        try {
            authServiceClient.byEmail(email);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist");
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to verify user right now");
        }
    }

    private void addMemberIfMissing(Room room, String userEmail) {
        roomMemberRepository.findByRoomAndUserEmail(room, userEmail)
                .orElseGet(() -> {
                    RoomMember member = new RoomMember();
                    member.setRoom(room);
                    member.setUserEmail(userEmail);
                    return roomMemberRepository.save(member);
                });
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getName(),
                room.getDescription(),
                room.getAvatarData(),
                room.getCreatedBy(),
                room.getCreatedAt()
        );
    }

    private RoomResponse toResponseForUser(Room room, String userEmail) {
        if (room == null) {
            return null;
        }

        if (room.getRoomCode() != null && room.getRoomCode().startsWith("dm-")) {
            String displayName = resolveDirectMessageDisplayName(room, userEmail);
            if (displayName != null && !displayName.isBlank()) {
                return new RoomResponse(
                        room.getId(),
                        room.getRoomCode(),
                        displayName,
                        room.getDescription(),
                        room.getAvatarData(),
                        room.getCreatedBy(),
                        room.getCreatedAt()
                );
            }
        }

        return toResponse(room);
    }

    private String resolveDirectMessageDisplayName(Room room, String userEmail) {
        try {
            List<RoomMember> members = roomMemberRepository.findByRoomOrderByJoinedAtAsc(room);
            if (members.size() != 2) {
                return "Direct message";
            }

            String otherEmail = members.stream()
                    .map(RoomMember::getUserEmail)
                    .filter(email -> email != null && !email.equalsIgnoreCase(userEmail))
                    .findFirst()
                    .orElse(null);

            if (otherEmail == null || otherEmail.isBlank()) {
                return "Direct message";
            }

            PublicUserProfileResponse otherUser = authServiceClient.byEmail(otherEmail);
            if (otherUser == null) {
                return otherEmail;
            }

            String name = otherUser.name();
            if (name != null && !name.isBlank()) {
                return normalizeRoomName(name);
            }

            return otherUser.email() != null ? otherUser.email() : otherEmail;
        } catch (Exception ignored) {
            return "Direct message";
        }
    }

    private String generateRoomCode(String name) {
        String base = sanitize(name);
        if (base.isBlank()) {
            base = "room";
        }
        String candidate = base;
        while (roomRepository.existsByRoomCode(candidate)) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidate;
    }

    private String sanitize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String normalizeRoomName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeAvatar(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        return trimmed;
    }

    private String dmRoomCode(String emailA, String emailB) {
        String a = String.valueOf(emailA).trim().toLowerCase(Locale.ROOT);
        String b = String.valueOf(emailB).trim().toLowerCase(Locale.ROOT);

        String left = a.compareTo(b) <= 0 ? a : b;
        String right = a.compareTo(b) <= 0 ? b : a;

        String input = left + "|" + right;
        byte[] hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            hash = input.getBytes(StandardCharsets.UTF_8);
        }

        StringBuilder hex = new StringBuilder();
        for (byte value : hash) {
            hex.append(String.format("%02x", value));
            if (hex.length() >= 12) break;
        }

        return "dm-" + hex;
    }
}
