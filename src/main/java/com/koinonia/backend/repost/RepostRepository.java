package com.koinonia.backend.repost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RepostRepository extends JpaRepository<Repost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Repost> findByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("SELECT r.post.id, COUNT(r) FROM Repost r WHERE r.post.id IN :postIds GROUP BY r.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT r.post.id FROM Repost r WHERE r.user.id = :userId AND r.post.id IN :postIds")
    Set<Long> findRepostedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
