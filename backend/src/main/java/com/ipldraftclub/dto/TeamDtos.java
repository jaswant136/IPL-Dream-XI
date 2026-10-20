package com.ipldraftclub.dto;

import com.ipldraftclub.entity.PlayerRole;

import java.util.List;
import java.util.UUID;

public final class TeamDtos {

    private TeamDtos() {
    }

    /** {@code seasons} lists only seasons the backend can actually draft (never an invented one). */
    public record TeamDto(UUID id, String code, String name, boolean active, List<Integer> seasons) {
    }

    public record TeamRefDto(UUID id, String code, String name) {
    }

    public record TeamSeasonsDto(String teamCode, String teamName, List<Integer> seasons) {
    }

    /**
     * One draftable roster entry. {@code id} is the player-season id (what picks refer to).
     * {@code rating} is null when it must stay hidden (Expert Mode while drafting, or public roster listing).
     */
    public record PlayerDto(
            UUID id,
            UUID playerId,
            String name,
            String country,
            PlayerRole role,
            boolean overseas,
            Integer rating,
            String battingStyle,
            String bowlingStyle,
            String teamCode,
            String teamName,
            int season) {
    }

    public record PlayerPoolDto(TeamRefDto team, int season, int playerCount, List<PlayerDto> players) {
    }
}
