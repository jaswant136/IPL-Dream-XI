package com.ipldraftclub.game;

import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.exception.RuleViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Pure rule enforcement. Has no database or web dependencies, so it is trivially unit-testable. */
@Component
public class SquadValidator {

    /**
     * Throws {@link RuleViolationException} if adding {@code candidate} to {@code current} would break a rule.
     * Also refuses picks that would make the remaining slots unable to satisfy the required roles.
     */
    public void validatePick(SquadRules rules, List<PlayerSeason> current, PlayerSeason candidate) {
        String name = candidate.getPlayer().getFullName();

        if (current.size() >= rules.squadSize()) {
            throw new RuleViolationException("Your XI is already full (" + rules.squadSize() + " players). Remove a player first.");
        }

        Set<UUID> playerIds = new HashSet<>();
        for (PlayerSeason ps : current) {
            if (ps.getId().equals(candidate.getId())) {
                throw new RuleViolationException(name + " is already in your XI.");
            }
            playerIds.add(ps.getPlayer().getId());
        }
        if (playerIds.contains(candidate.getPlayer().getId())) {
            throw new RuleViolationException(name + " is already in your XI (one player cannot be picked twice).");
        }

        if (rules.maxPlayerRating() != null && candidate.getRating() > rules.maxPlayerRating()) {
            throw new RuleViolationException(name + " is rated above this challenge's individual rating cap.");
        }

        long overseas = current.stream().filter(PlayerSeason::isOverseas).count();
        if (candidate.isOverseas() && overseas + 1 > rules.maxOverseas()) {
            throw new RuleViolationException("Overseas limit reached: at most " + rules.maxOverseas()
                    + " overseas players are allowed. " + name + " is an overseas player.");
        }

        if (rules.maxTotalRating() != null) {
            int total = current.stream().mapToInt(PlayerSeason::getRating).sum() + candidate.getRating();
            if (total > rules.maxTotalRating()) {
                throw new RuleViolationException("Budget exceeded: your XI's total rating cannot exceed "
                        + rules.maxTotalRating() + ".");
            }
        }

        if (rules.maxFromOneFranchise() != null) {
            long fromSame = current.stream()
                    .filter(ps -> ps.getTeam().getId().equals(candidate.getTeam().getId()))
                    .count();
            if (fromSame + 1 > rules.maxFromOneFranchise()) {
                throw new RuleViolationException("At most " + rules.maxFromOneFranchise()
                        + " players may come from " + candidate.getTeam().getName() + ".");
            }
        }

        // Dead-end guard: the slots left after this pick must still be able to cover every missing role.
        List<PlayerSeason> after = new ArrayList<>(current);
        after.add(candidate);
        int deficit = roleDeficit(rules, after);
        int slotsLeft = rules.squadSize() - after.size();
        if (deficit > slotsLeft) {
            throw new RuleViolationException("Picking " + name + " would leave too few slots for the required roles: "
                    + String.join(", ", missingRoleMessages(rules, after)) + ".");
        }
    }

    /** Returns every reason the XI is not yet a valid, completable squad. Empty list = valid. */
    public List<String> completionIssues(SquadRules rules, List<PlayerSeason> picks) {
        List<String> issues = new ArrayList<>();

        if (picks.size() != rules.squadSize()) {
            issues.add("Select exactly " + rules.squadSize() + " players (" + picks.size() + " selected).");
        }
        issues.addAll(missingRoleMessages(rules, picks).stream().map(m -> "Missing: " + m + ".").toList());

        long overseas = picks.stream().filter(PlayerSeason::isOverseas).count();
        if (overseas > rules.maxOverseas()) {
            issues.add("Too many overseas players: " + overseas + " selected, maximum " + rules.maxOverseas() + ".");
        }
        if (rules.maxTotalRating() != null) {
            int total = picks.stream().mapToInt(PlayerSeason::getRating).sum();
            if (total > rules.maxTotalRating()) {
                issues.add("Budget exceeded: total rating " + total + " is above " + rules.maxTotalRating() + ".");
            }
        }
        if (rules.maxPlayerRating() != null
                && picks.stream().anyMatch(ps -> ps.getRating() > rules.maxPlayerRating())) {
            issues.add("A selected player exceeds the individual rating cap of " + rules.maxPlayerRating() + ".");
        }
        if (rules.maxFromOneFranchise() != null) {
            picks.stream()
                    .collect(java.util.stream.Collectors.groupingBy(ps -> ps.getTeam().getName(), java.util.stream.Collectors.counting()))
                    .forEach((team, n) -> {
                        if (n > rules.maxFromOneFranchise()) {
                            issues.add("Too many players from " + team + ": " + n + ", maximum " + rules.maxFromOneFranchise() + ".");
                        }
                    });
        }
        Set<UUID> seen = new HashSet<>();
        for (PlayerSeason ps : picks) {
            if (!seen.add(ps.getPlayer().getId())) {
                issues.add(ps.getPlayer().getFullName() + " appears more than once.");
            }
        }
        return issues;
    }

    public static int count(List<PlayerSeason> picks, PlayerRole role) {
        return (int) picks.stream().filter(ps -> ps.getRole() == role).count();
    }

    private int roleDeficit(SquadRules rules, List<PlayerSeason> picks) {
        return Math.max(0, rules.minWicketkeepers() - count(picks, PlayerRole.WICKETKEEPER))
                + Math.max(0, rules.minBatters() - count(picks, PlayerRole.BATTER))
                + Math.max(0, rules.minBowlers() - count(picks, PlayerRole.BOWLER))
                + Math.max(0, rules.minAllRounders() - count(picks, PlayerRole.ALL_ROUNDER));
    }

    private List<String> missingRoleMessages(SquadRules rules, List<PlayerSeason> picks) {
        List<String> missing = new ArrayList<>();
        addMissing(missing, rules.minWicketkeepers(), count(picks, PlayerRole.WICKETKEEPER), "wicketkeeper");
        addMissing(missing, rules.minBatters(), count(picks, PlayerRole.BATTER), "specialist batter");
        addMissing(missing, rules.minAllRounders(), count(picks, PlayerRole.ALL_ROUNDER), "all-rounder");
        addMissing(missing, rules.minBowlers(), count(picks, PlayerRole.BOWLER), "specialist bowler");
        return missing;
    }

    private void addMissing(List<String> out, int required, int have, String label) {
        if (have < required) {
            int need = required - have;
            out.add(need + " more " + label + (need > 1 ? "s" : ""));
        }
    }
}
