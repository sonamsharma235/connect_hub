package com.connecthub.auth.service;

import com.connecthub.auth.domain.UserFavourite;
import com.connecthub.auth.repository.UserFavouriteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FavouriteService {

    private final UserFavouriteRepository repository;

    public FavouriteService(UserFavouriteRepository repository) {
        this.repository = repository;
    }

    public List<String> list(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required");
        }
        return repository.findAllByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(UserFavourite::getRoomCode)
                .distinct()
                .toList();
    }

    @Transactional
    public void add(String userEmail, String roomCode) {
        String normalizedEmail = normalizeEmail(userEmail);
        String normalizedRoomCode = normalizeRoomCode(roomCode);

        if (repository.existsByUserEmailAndRoomCode(normalizedEmail, normalizedRoomCode)) {
            return;
        }

        UserFavourite favourite = new UserFavourite();
        favourite.setUserEmail(normalizedEmail);
        favourite.setRoomCode(normalizedRoomCode);
        repository.save(favourite);
    }

    @Transactional
    public void remove(String userEmail, String roomCode) {
        String normalizedEmail = normalizeEmail(userEmail);
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        repository.deleteByUserEmailAndRoomCode(normalizedEmail, normalizedRoomCode);
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required");
        }
        return value.trim().toLowerCase();
    }

    private String normalizeRoomCode(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room code is required");
        }
        return value.trim();
    }
}

