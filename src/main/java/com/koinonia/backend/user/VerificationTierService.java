package com.koinonia.backend.user;

import com.koinonia.backend.follow.FollowRepository;
import com.koinonia.backend.like.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerificationTierService {

    private static final String FOUNDER_EMAIL_1 = "mauriciousfrimpong@gmail.com";
    private static final String FOUNDER_EMAIL_2 = "frimpongmauricious@gmail.com";

    private final FollowRepository followRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    public VerificationTier computeVerificationTier(User user) {
        if (user.getEmail().equalsIgnoreCase(FOUNDER_EMAIL_1) ||
                user.getEmail().equalsIgnoreCase(FOUNDER_EMAIL_2)) {
            return VerificationTier.GOLD;
        }

        long followerCount = followRepository.countByFollowingId(user.getId());
        long totalLikes = postLikeRepository.countByPostAuthorId(user.getId());

        if (followerCount >= 1000 && totalLikes >= 10000) {
            return VerificationTier.GOLD;
        }
        if (followerCount >= 100) {
            return VerificationTier.GREEN;
        }
        if (followerCount >= 50) {
            return VerificationTier.BLUE;
        }
        return VerificationTier.NONE;
    }

    @Transactional
    public void recomputeAndSave(User user) {
        VerificationTier tier = computeVerificationTier(user);
        if (user.getVerificationTier() != tier) {
            user.setVerificationTier(tier);
            userRepository.save(user);
        }
    }
}
