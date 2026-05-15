package com.connecthub.auth.repository;

import com.connecthub.auth.domain.UserFavourite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFavouriteRepository extends JpaRepository<UserFavourite, Long> {

    boolean existsByUserEmailAndRoomCode(String userEmail, String roomCode);

    List<UserFavourite> findAllByUserEmailOrderByCreatedAtDesc(String userEmail);

    long deleteByUserEmailAndRoomCode(String userEmail, String roomCode);
}

