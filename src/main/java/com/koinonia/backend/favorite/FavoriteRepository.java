package com.koinonia.backend.favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Favorite> findByUserIdAndPostId(Long userId, Long postId);

    Page<Favorite> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT f.post.id FROM Favorite f WHERE f.user.id = :userId AND f.post.id IN :postIds")
    Set<Long> findFavoritedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
