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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A player's membership of one franchise in one season. This is the unit that gets drafted.
 */
@Entity
@Table(name = "player_seasons")
@Getter
@Setter
@NoArgsConstructor
public class PlayerSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private int season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerRole role;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private boolean overseas;

    @Column(nullable = false)
    private boolean active = true;

    public PlayerSeason(Player player, Team team, int season, PlayerRole role, int rating, boolean overseas) {
        this.player = player;
        this.team = team;
        this.season = season;
        this.role = role;
        this.rating = rating;
        this.overseas = overseas;
        this.active = true;
    }
}
