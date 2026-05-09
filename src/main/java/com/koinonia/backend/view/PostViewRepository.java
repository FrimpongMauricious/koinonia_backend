package com.koinonia.backend.view;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostViewRepository extends JpaRepository<PostView, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    @Query("SELECT pv.post.id, COUNT(pv) FROM PostView pv WHERE pv.post.id IN :postIds GROUP BY pv.post.id")
    List<Object[]> countByPostIdIn(@Param("postIds") Collection<Long> postIds);

    @Modifying
    @Query(value = "INSERT INTO post_views (user_id, post_id) VALUES (:userId, :postId)", nativeQuery = true)
    void insertById(@Param("userId") Long userId, @Param("postId") Long postId);
}
