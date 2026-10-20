package com.ipldraftclub.dto;

import com.ipldraftclub.dto.TeamDtos.PlayerDto;
import com.ipldraftclub.dto.TeamDtos.TeamRefDto;
import com.ipldraftclub.entity.DraftStatus;
import com.ipldraftclub.entity.GameMode;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DraftDtos {

    private DraftDtos() {
    }

    public record StartDraftRequest(@NotNull(message = "mode is required") GameMode mode) {
    }

    public record AddPickRequest(@NotNull(message = "playerSeasonId is required") UUID playerSeasonId) {
    }

    public record RulesDto(
            int squadSize,
            int maxOverseas,
            int minWicketkeepers,
            int minBatters,
            int minBowlers,
            int minAllRounders,
            Integer maxTotalRating,
            Integer maxFromOneFranchise,
            Integer maxPlayerRating) {
    }

    public record GameModeDto(GameMode mode, String label, String description, boolean ratingsHiddenWhileDrafting,
                              RulesDto rules) {
    }

    public record PickDto(UUID pickId, int pickNumber, PlayerDto player) {
    }

    /**
     * Full draft state. {@code players} (the eligible pool) is included by start and get; it is null on
     * pick mutations to keep those responses small. Ratings inside are null for Expert Mode in progress.
     */
    public record DraftDetailDto(
            UUID draftId,
            GameMode mode,
            String modeLabel,
            TeamRefDto team,
            Integer season,
            boolean multiSeason,
            DraftStatus status,
            String challengeCode,
            RulesDto rules,
            boolean ratingsHidden,
            Instant startedAt,
            Instant completedAt,
            int pickCount,
            int squadSize,
            int overseasCount,
            boolean canComplete,
            List<String> issues,
            List<PickDto> picks,
            List<PlayerDto> players,
            boolean shared,
            String shareToken) {
    }

    public record DraftSummaryDto(
            UUID draftId,
            GameMode mode,
            String modeLabel,
            TeamRefDto team,
            Integer season,
            DraftStatus status,
            String challengeCode,
            Instant startedAt,
            Instant completedAt,
            int pickCount,
            Integer teamRating,
            Integer points,
            boolean shared,
            List<String> playerNames) {
    }

    public record DraftResultDto(
            UUID draftId,
            GameMode mode,
            String modeLabel,
            TeamRefDto team,
            Integer season,
            String challengeCode,
            int teamRating,
            int battingScore,
            int bowlingScore,
            int allRoundScore,
            int wicketkeepingScore,
            int roleBalanceScore,
            int overseasPlayers,
            int points,
            Long completionSeconds,
            boolean isComplete,
            List<String> validationMessages,
            Instant completedAt,
            List<PickDto> picks,
            boolean shared,
            String shareToken) {
    }

    public record ShareDto(String shareToken, String sharePath) {
    }

    /** Public view of a shared draft: no uid, no email, no profile information. */
    public record SharedDraftDto(
            GameMode mode,
            String modeLabel,
            TeamRefDto team,
            Integer season,
            String challengeCode,
            int teamRating,
            int battingScore,
            int bowlingScore,
            int allRoundScore,
            int wicketkeepingScore,
            int roleBalanceScore,
            int overseasPlayers,
            Instant completedAt,
            List<PickDto> picks) {
    }
}
