package com.koinonia.backend.post;

import com.koinonia.backend.comment.CommentRepository;
import com.koinonia.backend.exception.ForbiddenException;
import com.koinonia.backend.exception.PostNotFoundException;
import com.koinonia.backend.favorite.FavoriteRepository;
import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.like.PostLikeRepository;
import com.koinonia.backend.view.PostViewRepository;
import com.koinonia.backend.post.dto.CreatePostRequest;
import com.koinonia.backend.post.dto.PostResponse;
import com.koinonia.backend.post.dto.TopicCountResponse;
import com.koinonia.backend.post.dto.UpdatePostRequest;
import com.koinonia.backend.repost.RepostRepository;
import com.koinonia.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final RepostRepository repostRepository;
    private final FavoriteRepository favoriteRepository;
    private final FollowRepository followRepository;
    private final PostViewRepository postViewRepository;

    @Transactional
    public PostResponse createPost(CreatePostRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Post post = Post.builder()
                .user(currentUser)
                .title(request.getTitle())
                .topic(request.getTopic())
                .content(request.getContent())
                .build();
        return PostResponse.from(postRepository.save(post), 0L, 0L, false, 0L, false, false, false, 0L);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable, Topic topic) {
        Page<Post> page = topic != null
                ? postRepository.findByTopic(topic, pageable)
                : postRepository.findAll(pageable);
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<TopicCountResponse> getTopicCounts() {
        Map<Topic, Long> counts = postRepository.countByTopic().stream()
                .collect(Collectors.toMap(r -> (Topic) r[0], r -> (Long) r[1]));
        return Arrays.stream(Topic.values())
                .map(t -> TopicCountResponse.builder().topic(t).postCount(counts.getOrDefault(t, 0L)).build())
                .sorted(Comparator.comparingLong(TopicCountResponse::getPostCount).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Post findById(Long id) {
        return postRepository.findByIdWithUser(id)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByUser(Long userId, Pageable pageable) {
        Page<Post> page = postRepository.findByUserId(userId, pageable);
        return new PageImpl<>(toResponses(page.getContent()), pageable, page.getTotalElements());
    }

    @Transactional
    public PostResponse updatePost(Long id, UpdatePostRequest request, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        User currentUser = (User) authentication.getPrincipal();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not the author of this post");
        }
        post.setTitle(request.getTitle());
        post.setTopic(request.getTopic());
        post.setContent(request.getContent());
        // saveAndFlush triggers @PreUpdate so updatedAt is fresh in the response
        return toResponse(postRepository.saveAndFlush(post));
    }

    @Transactional
    public void deletePost(Long id, Authentication authentication) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));
        User currentUser = (User) authentication.getPrincipal();
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not the author of this post");
        }
        postRepository.delete(post);
    }

    // ── public enrichment API (used by RepostService and FavoriteService) ────────

    public List<PostResponse> enrichPosts(List<Post> posts) {
        return toResponses(posts);
    }

    // ── enrichment helpers ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PostResponse toResponse(Post post) {
        return toResponses(List.of(post)).get(0);
    }

    private List<PostResponse> toResponses(List<Post> posts) {
        if (posts.isEmpty()) return List.of();

        List<Long> ids = posts.stream().map(Post::getId).toList();
        User currentUser = getCurrentUser();

        Map<Long, Long> likeCounts = postLikeRepository.countByPostIds(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        Map<Long, Long> commentCounts = commentRepository.countByPostIds(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        Map<Long, Long> repostCounts = repostRepository.countByPostIds(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        Set<Long> likedIds = currentUser != null
                ? postLikeRepository.findLikedPostIds(currentUser.getId(), ids)
                : Set.of();

        Set<Long> repostedIds = currentUser != null
                ? repostRepository.findRepostedPostIds(currentUser.getId(), ids)
                : Set.of();

        Set<Long> favoritedIds = currentUser != null
                ? favoriteRepository.findFavoritedPostIds(currentUser.getId(), ids)
                : Set.of();

        Set<Long> authorIds = posts.stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> followedAuthorIds;
        if (currentUser != null) {
            Set<Long> otherAuthorIds = authorIds.stream()
                    .filter(id -> !id.equals(currentUser.getId()))
                    .collect(Collectors.toSet());
            followedAuthorIds = otherAuthorIds.isEmpty()
                    ? Set.of()
                    : followRepository.findFollowedAuthorIds(currentUser.getId(), otherAuthorIds);
        } else {
            followedAuthorIds = Set.of();
        }

        Map<Long, Long> viewCounts = postViewRepository.countByPostIdIn(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        return posts.stream()
                .map(p -> PostResponse.from(p,
                        likeCounts.getOrDefault(p.getId(), 0L),
                        commentCounts.getOrDefault(p.getId(), 0L),
                        likedIds.contains(p.getId()),
                        repostCounts.getOrDefault(p.getId(), 0L),
                        repostedIds.contains(p.getId()),
                        favoritedIds.contains(p.getId()),
                        followedAuthorIds.contains(p.getUser().getId()),
                        viewCounts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u;
        }
        return null;
    }
}
