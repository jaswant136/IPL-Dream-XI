package com.ipldraftclub.seed;

import com.ipldraftclub.entity.Player;
import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;
import com.ipldraftclub.repository.PlayerRepository;
import com.ipldraftclub.repository.PlayerSeasonRepository;
import com.ipldraftclub.repository.TeamRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validated, idempotent roster importer. This is the ONLY supported way to add player-season data.
 *
 * Required columns: player_name, country, team_code, season, role, rating
 * Optional columns: batting_style, bowling_style, default_role, overseas, active
 *
 * Every row is validated BEFORE anything is written: known franchise, plausible season, non-blank player,
 * valid role, rating 1-100, sane country, and an overseas flag consistent with the country.
 * Re-importing the same file never creates duplicates (existing player/franchise/season rows are skipped).
 * Lines starting with '#' are comments.
 */
@Service
public class RosterCsvImporter {

    private static final Pattern COUNTRY = Pattern.compile("^[\\p{L} .'-]{2,60}$");
    private static final int MIN_SEASON = 2008;
    private static final List<String> REQUIRED = List.of("player_name", "country", "team_code", "season", "role", "rating");

    private record Row(long record, String name, String country, String batting, String bowling, PlayerRole defaultRole,
                       String teamCode, int season, PlayerRole role, int rating, boolean overseas, boolean active) {
    }

    private final TeamRepository teams;
    private final PlayerRepository players;
    private final PlayerSeasonRepository playerSeasons;

    public RosterCsvImporter(TeamRepository teams, PlayerRepository players, PlayerSeasonRepository playerSeasons) {
        this.teams = teams;
        this.players = players;
        this.playerSeasons = playerSeasons;
    }

    /**
     * @param strict when true, any invalid row aborts the whole import and nothing is written
     */
    @Transactional
    public ImportSummary importCsv(Reader reader, String source, boolean strict) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .setCommentMarker('#')
                .build();

        List<String> messages = new ArrayList<>();
        List<Row> valid = new ArrayList<>();
        int total = 0;
        int invalid = 0;
        int skipped = 0;
        Set<String> seenKeys = new HashSet<>();
        Map<String, Team> teamsByCode = new HashMap<>();
        teams.findAll().forEach(t -> teamsByCode.put(t.getCode().toUpperCase(Locale.ROOT), t));
        int maxSeason = Year.now(ZoneOffset.UTC).getValue() + 1;

        try (CSVParser parser = format.parse(reader)) {
            List<String> header = parser.getHeaderNames().stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
            for (String col : REQUIRED) {
                if (!header.contains(col)) {
                    return new ImportSummary(source, 0, 0, 0, 1, true,
                            List.of("Missing required column '" + col + "'. Required: " + String.join(", ", REQUIRED)));
                }
            }
            for (CSVRecord rec : parser) {
                total++;
                try {
                    Row row = parse(rec, teamsByCode, maxSeason);
                    String key = row.name().toLowerCase(Locale.ROOT) + "|" + row.country().toLowerCase(Locale.ROOT)
                            + "|" + row.teamCode() + "|" + row.season();
                    if (!seenKeys.add(key)) {
                        skipped++;
                        messages.add("record " + rec.getRecordNumber() + ": duplicate of an earlier row in this file ("
                                + row.name() + " " + row.teamCode() + " " + row.season() + ") - skipped");
                        continue;
                    }
                    valid.add(row);
                } catch (IllegalArgumentException e) {
                    invalid++;
                    messages.add("record " + rec.getRecordNumber() + ": " + e.getMessage());
                }
            }
        }

        if (strict && invalid > 0) {
            return new ImportSummary(source, total, 0, skipped, invalid, true, messages);
        }

        int imported = 0;
        Map<String, Player> playerCache = new HashMap<>();
        for (Row row : valid) {
            Team team = teamsByCode.get(row.teamCode());
            String pKey = row.name().toLowerCase(Locale.ROOT) + "|" + row.country().toLowerCase(Locale.ROOT);
            Player player = playerCache.get(pKey);
            if (player == null) {
                player = players.findByFullNameIgnoreCaseAndCountryIgnoreCase(row.name(), row.country())
                        .orElseGet(() -> players.save(new Player(row.name(), row.country(), row.batting(),
                                row.bowling(), row.defaultRole())));
                playerCache.put(pKey, player);
            }
            if (playerSeasons.existsByPlayerIdAndTeamIdAndSeason(player.getId(), team.getId(), row.season())) {
                skipped++;
                continue;
            }
            PlayerSeason ps = new PlayerSeason(player, team, row.season(), row.role(), row.rating(), row.overseas());
            ps.setActive(row.active());
            playerSeasons.save(ps);
            imported++;
        }
        return new ImportSummary(source, total, imported, skipped, invalid, false, messages);
    }

    private Row parse(CSVRecord rec, Map<String, Team> teamsByCode, int maxSeason) {
        String name = value(rec, "player_name");
        if (name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("player_name is required (max 120 characters)");
        }
        String country = value(rec, "country");
        if (!COUNTRY.matcher(country).matches()) {
            throw new IllegalArgumentException("invalid country '" + country + "' for " + name);
        }
        String teamCode = value(rec, "team_code").toUpperCase(Locale.ROOT);
        if (!teamsByCode.containsKey(teamCode)) {
            throw new IllegalArgumentException("unknown franchise code '" + teamCode + "' for " + name);
        }
        int season;
        try {
            season = Integer.parseInt(value(rec, "season"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("season '" + value(rec, "season") + "' is not a number (" + name + ")");
        }
        if (season < MIN_SEASON || season > maxSeason) {
            throw new IllegalArgumentException("season " + season + " is outside " + MIN_SEASON + "-" + maxSeason
                    + " (" + name + ")");
        }
        PlayerRole role;
        try {
            role = PlayerRole.parse(value(rec, "role"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + " (" + name + ")");
        }
        int rating;
        try {
            rating = Integer.parseInt(value(rec, "rating"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("rating '" + value(rec, "rating") + "' is not a number (" + name + ")");
        }
        if (rating < 1 || rating > 100) {
            throw new IllegalArgumentException("rating " + rating + " must be 1-100 (" + name + ")");
        }

        PlayerRole defaultRole = null;
        String dr = value(rec, "default_role");
        if (!dr.isBlank()) {
            try {
                defaultRole = PlayerRole.parse(dr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("default_role: " + e.getMessage() + " (" + name + ")");
            }
        }

        boolean derivedOverseas = !country.equalsIgnoreCase("India");
        String overseasRaw = value(rec, "overseas");
        if (!overseasRaw.isBlank()) {
            boolean given = switch (overseasRaw.toLowerCase(Locale.ROOT)) {
                case "true", "yes", "y", "1" -> true;
                case "false", "no", "n", "0" -> false;
                default -> throw new IllegalArgumentException("overseas '" + overseasRaw + "' must be true or false ("
                        + name + ")");
            };
            if (given != derivedOverseas) {
                throw new IllegalArgumentException("overseas flag conflicts with country " + country + " (" + name + ")");
            }
        }
        String activeRaw = value(rec, "active");
        boolean active = activeRaw.isBlank() || !activeRaw.equalsIgnoreCase("false");

        return new Row(rec.getRecordNumber(), name, country, blankToNull(value(rec, "batting_style")),
                blankToNull(value(rec, "bowling_style")), defaultRole, teamCode, season, role, rating,
                derivedOverseas, active);
    }

    private static String value(CSVRecord rec, String column) {
        if (!rec.isMapped(column) || !rec.isSet(column)) {
            return "";
        }
        String v = rec.get(column);
        return v == null ? "" : v.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
