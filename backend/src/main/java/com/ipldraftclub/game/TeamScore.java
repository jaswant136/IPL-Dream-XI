package com.ipldraftclub.game;

/** Result of the deterministic scoring model. All values are integers in the range 0-100. */
public record TeamScore(
        int teamRating,
        int battingScore,
        int bowlingScore,
        int allRoundScore,
        int wicketkeepingScore,
        int roleBalanceScore,
        int completenessScore,
        int overseasPlayers) {
}
