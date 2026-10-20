package com.ipldraftclub.game;

import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.entity.PlayerSeason;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic team evaluation. The same XI always produces the same score; nothing here is random
 * and nothing is a statistical prediction of match results - it is a transparent, rating-weighted index.
 *
 * <pre>
 * Per-player rating r (1-100) is stored server-side on the roster record.
 *
 * battingScore       = weighted mean of r, weights  WK 1.0 | BATTER 1.0 | ALL_ROUNDER 0.6 | BOWLER 0.15
 * bowlingScore       = weighted mean of r, weights  BOWLER 1.0 | ALL_ROUNDER 0.6 | others 0
 * allRoundScore      = mean(r of all-rounders) * min(allRounderCount, 2) / 2      (0 when none)
 * wicketkeepingScore = rating of the best wicketkeeper                              (0 when none)
 * roleBalanceScore   = max(0, 100 - 8 * sum(|count(role) - ideal(role)|))
 *                      ideal XI shape: 1 WK, 4 BATTER, 2 ALL_ROUNDER, 4 BOWLER
 * completenessScore  = 100 * picks / squadSize
 *
 * teamRating = round( (0.30*batting + 0.30*bowling + 0.10*allRound + 0.10*wicketkeeping + 0.20*roleBalance)
 *                     * picks / squadSize )
 * </pre>
 * Overseas usage is reported (and limited by the rules) but is not added to the rating.
 */
@Component
public class TeamScorer {

    static final int IDEAL_WK = 1;
    static final int IDEAL_BAT = 4;
    static final int IDEAL_AR = 2;
    static final int IDEAL_BOWL = 4;

    public TeamScore score(SquadRules rules, List<PlayerSeason> picks) {
        int size = picks.size();
        int overseas = (int) picks.stream().filter(PlayerSeason::isOverseas).count();
        if (size == 0) {
            return new TeamScore(0, 0, 0, 0, 0, 0, 0, 0);
        }

        double batting = weightedMean(picks, Weights.BATTING);
        double bowling = weightedMean(picks, Weights.BOWLING);

        List<PlayerSeason> allRounders = picks.stream().filter(p -> p.getRole() == PlayerRole.ALL_ROUNDER).toList();
        double allRound = allRounders.isEmpty() ? 0
                : allRounders.stream().mapToInt(PlayerSeason::getRating).average().orElse(0)
                * Math.min(allRounders.size(), 2) / 2.0;

        double keeping = picks.stream().filter(p -> p.getRole() == PlayerRole.WICKETKEEPER)
                .mapToInt(PlayerSeason::getRating).max().orElse(0);

        int deviation = Math.abs(SquadValidator.count(picks, PlayerRole.WICKETKEEPER) - IDEAL_WK)
                + Math.abs(SquadValidator.count(picks, PlayerRole.BATTER) - IDEAL_BAT)
                + Math.abs(SquadValidator.count(picks, PlayerRole.ALL_ROUNDER) - IDEAL_AR)
                + Math.abs(SquadValidator.count(picks, PlayerRole.BOWLER) - IDEAL_BOWL);
        double balance = Math.max(0, 100 - 8.0 * deviation);

        double completeness = 100.0 * Math.min(size, rules.squadSize()) / rules.squadSize();

        double raw = 0.30 * batting + 0.30 * bowling + 0.10 * allRound + 0.10 * keeping + 0.20 * balance;
        double rating = raw * Math.min(size, rules.squadSize()) / rules.squadSize();

        return new TeamScore(clamp(rating), clamp(batting), clamp(bowling), clamp(allRound),
                clamp(keeping), clamp(balance), clamp(completeness), overseas);
    }

    private double weightedMean(List<PlayerSeason> picks, Weights weights) {
        double sum = 0;
        double weightTotal = 0;
        for (PlayerSeason ps : picks) {
            double w = weights.of(ps.getRole());
            sum += w * ps.getRating();
            weightTotal += w;
        }
        return weightTotal == 0 ? 0 : sum / weightTotal;
    }

    private int clamp(double value) {
        return (int) Math.max(0, Math.min(100, Math.round(value)));
    }

    private enum Weights {
        BATTING(1.0, 1.0, 0.6, 0.15),
        BOWLING(0.0, 0.0, 0.6, 1.0);

        private final double wk;
        private final double bat;
        private final double ar;
        private final double bowl;

        Weights(double wk, double bat, double ar, double bowl) {
            this.wk = wk;
            this.bat = bat;
            this.ar = ar;
            this.bowl = bowl;
        }

        double of(PlayerRole role) {
            return switch (role) {
                case WICKETKEEPER -> wk;
                case BATTER -> bat;
                case ALL_ROUNDER -> ar;
                case BOWLER -> bowl;
            };
        }
    }
}
