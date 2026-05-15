package com.connecthub.room.repository;

import com.connecthub.room.domain.Room;
import com.connecthub.room.domain.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    Optional<RoomMember> findByRoomAndUserEmail(Room room, String userEmail);

    List<RoomMember> findByRoomOrderByJoinedAtAsc(Room room);

    boolean existsByRoomAndUserEmail(Room room, String userEmail);

    long deleteByRoom(Room room);

    long deleteByRoomAndUserEmail(Room room, String userEmail);

    @Query("select m.room from RoomMember m where m.userEmail = :userEmail order by m.joinedAt desc")
    List<Room> findRoomsForUser(@Param("userEmail") String userEmail);
}
