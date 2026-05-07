package com.koinonia.backend.follow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long followingId);  // how many users follow this user

    long countByFollowerId(Long followerId);    // how many users this user follows

    Page<Follow> findByFollowingId(Long followingId, Pageable pageable);  // followers of a user

    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);    // users a user follows
}
