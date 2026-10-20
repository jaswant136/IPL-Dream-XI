package com.ipldraftclub.game;

import com.ipldraftclub.entity.PlayerRole;

/**
 * Role/overseas makeup of a roster. Used to decide whether a franchise-season (or the Dream XI pool)
 * is actually draftable under a rule set, so the spin can never land on an unplayable combination.
 */
public record PoolComposition(int total, int wicketkeepers, int batters, int allRounders, int bowlers, int overseas) {

    public static PoolComposition empty() {
        return new PoolComposition(0, 0, 0, 0, 0, 0);
    }

    public PoolComposition plus(PlayerRole role, boolean isOverseas, int count) {
        return new PoolComposition(
                total + count,
                wicketkeepers + (role == PlayerRole.WICKETKEEPER ? count : 0),
                batters + (role == PlayerRole.BATTER ? count : 0),
                allRounders + (role == PlayerRole.ALL_ROUNDER ? count : 0),
                bowlers + (role == PlayerRole.BOWLER ? count : 0),
                overseas + (isOverseas ? count : 0));
    }

    public int domestic() {
        return total - overseas;
    }

    /** True when a valid XI under {@code rules} can be built from this pool. */
    public boolean isDraftable(SquadRules rules) {
        return total >= rules.squadSize()
                && wicketkeepers >= rules.minWicketkeepers()
                && batters >= rules.minBatters()
                && bowlers >= rules.minBowlers()
                && allRounders >= rules.minAllRounders()
                && domestic() >= rules.squadSize() - rules.maxOverseas();
    }
}
