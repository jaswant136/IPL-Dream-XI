package com.ipldraftclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "draft_picks")
@Getter
@Setter
@NoArgsConstructor
public class DraftPick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false, updatable = false)
    private Draft draft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_season_id", nullable = false, updatable = false)
    private PlayerSeason playerSeason;

    @Column(name = "pick_number", nullable = false)
    private int pickNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DraftPick(Draft draft, PlayerSeason playerSeason, int pickNumber) {
        this.draft = draft;
        this.playerSeason = playerSeason;
        this.pickNumber = pickNumber;
        this.createdAt = Instant.now();
    }
}
