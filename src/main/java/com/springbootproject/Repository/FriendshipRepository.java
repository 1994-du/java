package com.springbootproject.Repository;

import com.springbootproject.Entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserId(Long userId);

    List<Friendship> findByFriendId(Long friendId);

    List<Friendship> findByUserIdAndStatus(Long userId, String status);

    List<Friendship> findByFriendIdAndStatus(Long friendId, String status);

    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :userId OR f.friendId = :userId) AND f.status = :status")
    List<Friendship> findFriendsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :userId OR f.friendId = :userId)")
    List<Friendship> findAllByUserIdOrFriendId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE ((f.userId = :userId AND f.friendId = :friendId) OR (f.userId = :friendId AND f.friendId = :userId)) AND f.status = 'accepted'")
    boolean areFriends(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :userId AND f.friendId = :friendId) OR (f.userId = :friendId AND f.friendId = :userId)")
    Optional<Friendship> findFriendshipBetweenUsers(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
