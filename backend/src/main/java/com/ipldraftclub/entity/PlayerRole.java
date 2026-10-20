package com.ipldraftclub.entity;

public enum PlayerRole {
    WICKETKEEPER,
    BATTER,
    ALL_ROUNDER,
    BOWLER;

    /** Lenient parser used by the CSV importer (accepts "Wicket-keeper", "all rounder", ...). */
    public static PlayerRole parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("role is required");
        }
        String normalised = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalised) {
            case "WK", "WICKETKEEPER", "WICKET_KEEPER" -> WICKETKEEPER;
            case "BAT", "BATTER", "BATSMAN" -> BATTER;
            case "AR", "ALL_ROUNDER", "ALLROUNDER" -> ALL_ROUNDER;
            case "BOWL", "BOWLER" -> BOWLER;
            default -> throw new IllegalArgumentException("unknown role '" + raw + "'");
        };
    }
}
