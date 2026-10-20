package com.ipldraftclub.dto;

/** Public leaderboard row. Deliberately contains no email address and no Firebase UID. */
public record LeaderboardEntryDto(
        int rank,
        String displayName,
        int completedDrafts,
        double averageRating,
        int highestRating,
        long points) {

    public LeaderboardEntryDto withRank(int newRank) {
        return new LeaderboardEntryDto(newRank, displayName, completedDrafts, averageRating, highestRating, points);
    }
}
