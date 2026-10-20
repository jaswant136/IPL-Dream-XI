package com.ipldraftclub.repository;

import com.ipldraftclub.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByFirebaseUid(String firebaseUid);

    boolean existsByFirebaseUid(String firebaseUid);
}
