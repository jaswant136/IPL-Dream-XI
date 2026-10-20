package com.ipldraftclub.entity;

public enum GameMode {
    CLASSIC("Classic Draft", "Spin a franchise and season, then draft your XI with ratings visible."),
    EXPERT("Expert Mode", "Same draft, but ratings stay hidden until you lock in your XI."),
    CHALLENGE("Random Challenge", "Tighter rules: fewer overseas players, a rating budget and an all-rounder requirement."),
    DREAM("Dream XI", "A custom multi-season XI drawn from every roster in the database.");

    private final String label;
    private final String description;

    GameMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /** Whether player ratings are hidden from the client while the draft is in progress. */
    public boolean hidesRatingsWhileDrafting() {
        return this == EXPERT;
    }
}
