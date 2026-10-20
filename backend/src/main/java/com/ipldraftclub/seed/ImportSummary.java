package com.ipldraftclub.seed;

import java.util.List;

/**
 * Outcome of a roster import.
 *
 * @param imported new player-season rows written
 * @param skipped  valid rows that already existed (re-running an import is safe)
 * @param invalid  rows rejected by validation
 * @param aborted  true when strict mode found invalid rows and therefore wrote nothing
 */
public record ImportSummary(String source, int totalRows, int imported, int skipped, int invalid,
                            boolean aborted, List<String> messages) {

    public String describe() {
        return "Roster import [" + source + "]: " + totalRows + " rows, " + imported + " imported, "
                + skipped + " skipped (already present), " + invalid + " invalid"
                + (aborted ? " - ABORTED (strict mode), nothing written" : "");
    }
}
