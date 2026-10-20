package com.ipldraftclub.game;

import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;
import com.ipldraftclub.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.ipldraftclub.support.Fixtures.ps;
import static org.assertj.core.api.Assertions.assertThat;

class TeamScorerTest {

    private final TeamScorer scorer = new TeamScorer();
    private final Team team = Fixtures.team("TST");

    @Test
    void idealUniformXiScoresExactlyItsPlayerRating() {
        // All players rated 80 in the ideal 1/4/2/4 shape: batting=bowling=allRound=keeping=80, balance=100
        // teamRating = 0.30*80 + 0.30*80 + 0.10*80 + 0.10*80 + 0.20*100 = 84
        TeamScore s = scorer.score(RuleSets.STANDARD, Fixtures.idealXi(team, 80));
        assertThat(s.battingScore()).isEqualTo(80);
        assertThat(s.bowlingScore()).isEqualTo(80);
        assertThat(s.allRoundScore()).isEqualTo(80);
        assertThat(s.wicketkeepingScore()).isEqualTo(80);
        assertThat(s.roleBalanceScore()).isEqualTo(100);
        assertThat(s.completenessScore()).isEqualTo(100);
        assertThat(s.teamRating()).isEqualTo(84);
        assertThat(s.overseasPlayers()).isZero();
    }

    @Test
    void scoringIsDeterministic() {
        List<PlayerSeason> xi = Fixtures.idealXi(team, 77);
        assertThat(scorer.score(RuleSets.STANDARD, xi)).isEqualTo(scorer.score(RuleSets.STANDARD, xi));
    }

    @Test
    void betterPlayersGiveHigherRating() {
        int low = scorer.score(RuleSets.STANDARD, Fixtures.idealXi(team, 60)).teamRating();
        int high = scorer.score(RuleSets.STANDARD, Fixtures.idealXi(team, 90)).teamRating();
        assertThat(high).isGreaterThan(low);
    }

    @Test
    void unbalancedRolesLowerTheBalanceScore() {
        List<PlayerSeason> xi = new ArrayList<>();
        xi.add(ps("WK", PlayerRole.WICKETKEEPER, 80, false, team));
        for (int i = 0; i < 6; i++) {
            xi.add(ps("Bat " + i, PlayerRole.BATTER, 80, false, team));
        }
        for (int i = 0; i < 4; i++) {
            xi.add(ps("Bowl " + i, PlayerRole.BOWLER, 80, false, team));
        }
        TeamScore s = scorer.score(RuleSets.STANDARD, xi);
        // deviations: WK 0, BAT 2, AR 2, BOWL 0 = 4 -> 100 - 32 = 68
        assertThat(s.roleBalanceScore()).isEqualTo(68);
        assertThat(s.allRoundScore()).isZero();
    }

    @Test
    void incompleteXiIsScaledByCompleteness() {
        List<PlayerSeason> half = Fixtures.idealXi(team, 80).subList(0, 5);
        TeamScore s = scorer.score(RuleSets.STANDARD, half);
        assertThat(s.completenessScore()).isEqualTo(45);
        assertThat(s.teamRating()).isLessThan(50);
    }

    @Test
    void emptyXiScoresZeroAndAllScoresStayWithinBounds() {
        assertThat(scorer.score(RuleSets.STANDARD, List.of()).teamRating()).isZero();
        TeamScore max = scorer.score(RuleSets.STANDARD, Fixtures.idealXi(team, 100));
        assertThat(max.teamRating()).isBetween(0, 100);
    }

    @Test
    void overseasPlayersAreCounted() {
        List<PlayerSeason> xi = Fixtures.idealXi(team, 80);
        xi.set(1, ps("Import", PlayerRole.BATTER, 80, true, team));
        assertThat(scorer.score(RuleSets.STANDARD, xi).overseasPlayers()).isEqualTo(1);
    }
}
