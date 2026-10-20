package com.ipldraftclub.service;

import com.ipldraftclub.dto.UserDtos.MeDto;
import com.ipldraftclub.entity.UserProfile;
import com.ipldraftclub.entity.UserStatistics;
import com.ipldraftclub.exception.ApiException;
import com.ipldraftclub.repository.UserProfileRepository;
import com.ipldraftclub.repository.UserStatisticsRepository;
import com.ipldraftclub.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Local application profile for a Firebase user. Created lazily the first time an authenticated request
 * is verified. Passwords are never stored here - Firebase owns credentials.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserProfileRepository profiles;
    private final UserStatisticsRepository statistics;
    private final TransactionTemplate tx;

    public UserService(UserProfileRepository profiles, UserStatisticsRepository statistics,
                       PlatformTransactionManager txManager) {
        this.profiles = profiles;
        this.statistics = statistics;
        this.tx = new TransactionTemplate(txManager);
    }

    public void ensureProfile(AuthenticatedUser user) {
        String uid = user.uid();
        if (!profiles.existsByFirebaseUid(uid)) {
            try {
                tx.executeWithoutResult(status -> {
                    profiles.saveAndFlush(new UserProfile(uid, defaultDisplayName(user), user.email()));
                    statistics.saveAndFlush(new UserStatistics(uid));
                });
                log.info("Created local profile for a new user");
            } catch (DataIntegrityViolationException raced) {
                log.debug("Profile was created concurrently; continuing");
            }
        }
    }

    @Transactional(readOnly = true)
    public MeDto getMe(AuthenticatedUser user) {
        UserProfile p = profiles.findByFirebaseUid(user.uid())
                .orElseThrow(() -> ApiException.notFound("Profile not found."));
        return new MeDto(p.getDisplayName(), p.getEmail(), user.emailVerified(), p.getCreatedAt());
    }

    @Transactional
    public MeDto updateDisplayName(AuthenticatedUser user, String displayName) {
        UserProfile p = profiles.findByFirebaseUid(user.uid())
                .orElseThrow(() -> ApiException.notFound("Profile not found."));
        p.setDisplayName(displayName.trim());
        p.setUpdatedAt(java.time.Instant.now());
        profiles.save(p);
        return new MeDto(p.getDisplayName(), p.getEmail(), user.emailVerified(), p.getCreatedAt());
    }

    /**
     * Public-facing default name. Deliberately never derived from the e-mail address, so the
     * leaderboard cannot leak it.
     */
    static String defaultDisplayName(AuthenticatedUser user) {
        if (StringUtils.hasText(user.name())) {
            String cleaned = user.name().trim().replaceAll("[^\\p{L}\\p{N} _.'-]", "");
            if (cleaned.length() >= 2) {
                return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
            }
        }
        int suffix = Math.floorMod(user.uid().hashCode(), 10_000);
        return "Player" + String.format("%04d", suffix);
    }
}
