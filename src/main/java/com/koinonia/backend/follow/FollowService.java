package com.koinonia.backend.follow;

import com.koinonia.backend.exception.BadRequestException;
import com.koinonia.backend.exception.UserNotFoundException;
import com.koinonia.backend.follow.dto.FollowResponse;
import com.koinonia.backend.notification.NotificationService;
import com.koinonia.backend.user.User;
import com.koinonia.backend.user.UserRepository;
import com.koinonia.backend.user.VerificationTierService;
import com.koinonia.backend.user.dto.PublicUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final VerificationTierService verificationTierService;

    @Transactional
    public FollowResponse follow(User currentUser, Long targetUserId) {
        if (currentUser.getId().equals(targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
        if (!followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)) {
            followRepository.save(Follow.builder()
                    .follower(currentUser)
                    .following(target)
                    .build());
            notificationService.emitFollow(currentUser, target);
        }
        verificationTierService.recomputeAndSave(target);
        return buildFollowResponse(target, currentUser);
    }

    @Transactional
    public FollowResponse unfollow(User currentUser, Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
        followRepository.findByFollowerIdAndFollowingId(currentUser.getId(), targetUserId)
                .ifPresent(followRepository::delete);
        verificationTierService.recomputeAndSave(target);
        return buildFollowResponse(target, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<PublicUserResponse> getFollowers(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return followRepository.findByFollowingId(userId, pageable)
                .map(f -> PublicUserResponse.from(f.getFollower()));
    }

    @Transactional(readOnly = true)
    public Page<PublicUserResponse> getFollowing(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return followRepository.findByFollowerId(userId, pageable)
                .map(f -> PublicUserResponse.from(f.getFollowing()));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private FollowResponse buildFollowResponse(User target, User currentUser) {
        long followerCount  = followRepository.countByFollowingId(target.getId());
        long followingCount = followRepository.countByFollowerId(target.getId());
        boolean followedByMe = followRepository.existsByFollowerIdAndFollowingId(
                currentUser.getId(), target.getId());
        return FollowResponse.builder()
                .userId(target.getId())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .followedByCurrentUser(followedByMe)
                .build();
    }
}
