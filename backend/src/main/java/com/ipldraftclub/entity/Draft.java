package com.ipldraftclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drafts")
@Getter
@Setter
@NoArgsConstructor
public class Draft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "firebase_uid", nullable = false, length = 128, updatable = false)
    private String firebaseUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private GameMode mode;

    /** Null for multi-franchise modes (Dream XI). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", updatable = false)
    private Team team;

    /** Null for multi-season modes (Dream XI). */
    @Column(updatable = false)
    private Integer season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DraftStatus status = DraftStatus.IN_PROGRESS;

    @Column(name = "challenge_code", length = 20, updatable = false)
    private String challengeCode;

    @Column(name = "idempotency_key", length = 64, updatable = false)
    private String idempotencyKey;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completion_seconds")
    private Long completionSeconds;

    @Column(name = "team_rating")
    private Integer teamRating;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column
    private Integer points;

    @Column(name = "batting_score")
    private Integer battingScore;

    @Column(name = "bowling_score")
    private Integer bowlingScore;

    @Column(name = "all_round_score")
    private Integer allRoundScore;

    @Column(name = "wicketkeeping_score")
    private Integer wicketkeepingScore;

    @Column(name = "role_balance_score")
    private Integer roleBalanceScore;

    @Column(name = "overseas_count")
    private Integer overseasCount;

    @Column(nullable = false)
    private boolean shared = false;

    @Column(name = "share_token", length = 64)
    private String shareToken;

    /** Optimistic lock: guards against concurrent mutation on top of the pessimistic row lock. */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isCompleted() {
        return status == DraftStatus.COMPLETED;
    }
}
