package com.ipldraftclub.game;

/**
 * A complete, centrally defined rule set for one kind of draft.
 * All fields marked nullable are optional challenge rules and are ignored when null.
 *
 * @param squadSize           exact number of players in a completed XI
 * @param maxOverseas         maximum overseas players
 * @param minWicketkeepers    minimum players whose role is WICKETKEEPER
 * @param minBatters          minimum specialist batters (role BATTER)
 * @param minBowlers          minimum specialist bowlers (role BOWLER)
 * @param minAllRounders      minimum all-rounders
 * @param maxTotalRating      budget: maximum sum of player ratings in the XI (nullable)
 * @param maxFromOneFranchise maximum players from a single franchise (nullable; relevant to multi-franchise modes)
 * @param maxPlayerRating     highest individual rating that may be picked (nullable)
 */
public record SquadRules(
        int squadSize,
        int maxOverseas,
        int minWicketkeepers,
        int minBatters,
        int minBowlers,
        int minAllRounders,
        Integer maxTotalRating,
        Integer maxFromOneFranchise,
        Integer maxPlayerRating) {

    public int requiredRoleSlots() {
        return minWicketkeepers + minBatters + minBowlers + minAllRounders;
    }
}
