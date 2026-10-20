package com.ipldraftclub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 60)
    private String country;

    @Column(name = "batting_style", length = 40)
    private String battingStyle;

    @Column(name = "bowling_style", length = 60)
    private String bowlingStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_role", length = 20)
    private PlayerRole defaultRole;

    public Player(String fullName, String country, String battingStyle, String bowlingStyle, PlayerRole defaultRole) {
        this.fullName = fullName;
        this.country = country;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
        this.defaultRole = defaultRole;
    }
}
