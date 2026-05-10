package com.koinonia.backend.user;

import com.koinonia.backend.exception.BadRequestException;
import com.koinonia.backend.exception.UserNotFoundException;
import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.like.PostLikeRepository;
import com.koinonia.backend.user.dto.DeleteAccountRequest;
import com.koinonia.backend.user.dto.PublicUserProfileResponse;
import com.koinonia.backend.user.dto.UpdateProfileRequest;
import com.koinonia.backend.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostLikeRepository postLikeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User currentUser) {
        long followerCount  = followRepository.countByFollowingId(currentUser.getId());
        long followingCount = followRepository.countByFollowerId(currentUser.getId());
        long totalLikes     = postLikeRepository.countByPostAuthorId(currentUser.getId());
        return UserProfileResponse.from(currentUser, followerCount, followingCount, false, totalLikes);
    }

    @Transactional(readOnly = true)
    public PublicUserProfileResponse getPublicProfile(Long userId, User currentUser) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        long followerCount  = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        long totalLikes     = postLikeRepository.countByPostAuthorId(userId);

        boolean followedByCurrentUser = currentUser != null
                && !currentUser.getId().equals(userId)
                && followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), userId);

        return PublicUserProfileResponse.from(target, followerCount, followingCount, followedByCurrentUser, totalLikes);
    }

    @Transactional
    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        if (request.getDisplayName() != null) {
            currentUser.setDisplayName(request.getDisplayName().isBlank() ? null : request.getDisplayName());
        }
        if (request.getBio() != null) {
            currentUser.setBio(request.getBio().isBlank() ? null : request.getBio());
        }
        if (request.getProfilePictureUrl() != null) {
            currentUser.setProfilePictureUrl(request.getProfilePictureUrl().isBlank() ? null : request.getProfilePictureUrl());
        }

        User saved = userRepository.saveAndFlush(currentUser);

        long followerCount  = followRepository.countByFollowingId(saved.getId());
        long followingCount = followRepository.countByFollowerId(saved.getId());
        long totalLikes     = postLikeRepository.countByPostAuthorId(saved.getId());
        return UserProfileResponse.from(saved, followerCount, followingCount, false, totalLikes);
    }

    @Transactional(readOnly = true)
    public Page<PublicUserProfileResponse> searchUsers(String q, User currentUser) {
        List<User> users = userRepository.searchByUsername(q, PageRequest.of(0, 20));
        if (users.isEmpty()) return Page.empty();

        List<Long> userIds = users.stream().map(User::getId).toList();

        Map<Long, Long> followerCounts = followRepository.countFollowersByUserIds(userIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Map<Long, Long> followingCounts = followRepository.countFollowingByUserIds(userIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Map<Long, Long> likeCounts = postLikeRepository.countLikesByAuthorIds(userIds).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Set<Long> followedIds = currentUser != null
                ? followRepository.findFollowedAuthorIds(currentUser.getId(), new HashSet<>(userIds))
                : Set.of();

        List<PublicUserProfileResponse> sorted = users.stream()
                .sorted(Comparator.comparingLong((User u) -> followerCounts.getOrDefault(u.getId(), 0L)).reversed())
                .map(u -> PublicUserProfileResponse.from(u,
                        followerCounts.getOrDefault(u.getId(), 0L),
                        followingCounts.getOrDefault(u.getId(), 0L),
                        followedIds.contains(u.getId()),
                        likeCounts.getOrDefault(u.getId(), 0L)))
                .toList();

        return new PageImpl<>(sorted, PageRequest.of(0, 20), sorted.size());
    }

    @Transactional
    public void deleteAccount(User currentUser, DeleteAccountRequest request) {
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPasswordHash())) {
            throw new BadRequestException("Incorrect password");
        }
        userRepository.delete(currentUser);
    }
}
