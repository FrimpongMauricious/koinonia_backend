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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void deleteAccount(User currentUser, DeleteAccountRequest request) {
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPasswordHash())) {
            throw new BadRequestException("Incorrect password");
        }
        userRepository.delete(currentUser);
    }
}
