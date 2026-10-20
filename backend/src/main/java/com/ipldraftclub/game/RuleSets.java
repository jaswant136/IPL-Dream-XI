package com.ipldraftclub.game;

import com.ipldraftclub.entity.GameMode;

/**
 * Single place where per-mode rules are configured. Change a number here and every layer
 * (validation, pool eligibility, scoring, UI hints via /game-modes) follows.
 */
public final class RuleSets {

    /** Standard IPL-style XI: 11 players, max 4 overseas, WK + batter + 3 bowlers. */
    public static final SquadRules STANDARD = new SquadRules(11, 4, 1, 1, 3, 0, null, null, null);

    /** Random Challenge: max 3 overseas, an all-rounder is mandatory, and a rating budget applies. */
    public static final SquadRules CHALLENGE = new SquadRules(11, 3, 1, 1, 3, 1, 880, null, null);

    /** Dream XI spans seasons/franchises, so a single franchise may not dominate the XI. */
    public static final SquadRules DREAM = new SquadRules(11, 4, 1, 1, 3, 0, null, 5, null);

    private RuleSets() {
    }

    public static SquadRules forMode(GameMode mode) {
        return switch (mode) {
            case CLASSIC, EXPERT -> STANDARD;
            case CHALLENGE -> CHALLENGE;
            case DREAM -> DREAM;
        };
    }

    /** Leaderboard multiplier: harder modes are worth more points per team rating. */
    public static double pointsMultiplier(GameMode mode) {
        return switch (mode) {
            case CLASSIC, DREAM -> 1.0;
            case EXPERT -> 1.25;
            case CHALLENGE -> 1.5;
        };
    }
}
