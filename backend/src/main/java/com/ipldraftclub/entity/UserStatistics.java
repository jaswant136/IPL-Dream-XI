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

@Entity
@Table(name = "user_statistics")
@Getter
@Setter
@NoArgsConstructor
public class UserStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128, unique = true, updatable = false)
    private String firebaseUid;

    @Column(name = "total_drafts", nullable = false)
    private int totalDrafts;

    @Column(name = "completed_drafts", nullable = false)
    private int completedDrafts;

    @Column(name = "rating_sum", nullable = false)
    private long ratingSum;

    @Column(name = "average_rating", nullable = false)
    private double averageRating;

    @Column(name = "highest_rating", nullable = false)
    private int highestRating;

    @Column(nullable = false)
    private long points;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserStatistics(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public void recordStarted() {
        totalDrafts++;
        updatedAt = Instant.now();
    }

    public void recordCompleted(int teamRating, int draftPoints) {
        completedDrafts++;
        ratingSum += teamRating;
        averageRating = Math.round((ratingSum * 100.0) / completedDrafts) / 100.0;
        highestRating = Math.max(highestRating, teamRating);
        points += draftPoints;
        updatedAt = Instant.now();
    }
}
