package com.ipldraftclub.dto;

import com.ipldraftclub.dto.DraftDtos.DraftSummaryDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record MeDto(String displayName, String email, boolean emailVerified, Instant createdAt) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "displayName is required")
            @Size(min = 2, max = 40, message = "displayName must be 2-40 characters")
            @Pattern(regexp = "^[\\p{L}\\p{N} _.'-]+$", message = "displayName contains unsupported characters")
            String displayName) {
    }

    public record UserStatisticsDto(
            int totalDrafts,
            int completedDrafts,
            double averageRating,
            int highestRating,
            long points,
            Integer leaderboardPosition,
            DraftSummaryDto bestDraft) {
    }
}
