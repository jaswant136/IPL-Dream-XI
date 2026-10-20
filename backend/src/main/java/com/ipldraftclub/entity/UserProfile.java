package com.ipldraftclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Local profile for a Firebase user. Never stores passwords. */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128, unique = true, updatable = false)
    private String firebaseUid;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Column(length = 254)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserProfile(String firebaseUid, String displayName, String email) {
        this.firebaseUid = firebaseUid;
        this.displayName = displayName;
        this.email = email;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
