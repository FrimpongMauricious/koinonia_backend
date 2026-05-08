package com.koinonia.backend.follow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long followingId);  // how many users follow this user

    long countByFollowerId(Long followerId);    // how many users this user follows

    Page<Follow> findByFollowingId(Long followingId, Pageable pageable);  // followers of a user

    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);    // users a user follows

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId AND f.following.id IN :authorIds")
    Set<Long> findFollowedAuthorIds(@Param("followerId") Long followerId,
                                    @Param("authorIds") Collection<Long> authorIds);
}
