package com.ipldraftclub.game;

import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;
import com.ipldraftclub.exception.RuleViolationException;
import com.ipldraftclub.support.Fixtures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.ipldraftclub.support.Fixtures.ps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SquadValidatorTest {

    private final SquadValidator validator = new SquadValidator();
    private final SquadRules rules = RuleSets.STANDARD;
    private final Team team = Fixtures.team("TST");

    @Test
    void acceptsValidPick() {
        PlayerSeason wk = ps("Keeper", PlayerRole.WICKETKEEPER, 80, false, team);
        assertThatCode(() -> validator.validatePick(rules, List.of(), wk)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicatePlayerSeason() {
        PlayerSeason bat = ps("Batter", PlayerRole.BATTER, 80, false, team);
        assertThatThrownBy(() -> validator.validatePick(rules, List.of(bat), bat))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("already in your XI");
    }

    @Test
    void rejectsSamePlayerFromAnotherSeason() {
        PlayerSeason first = ps("Twice", PlayerRole.BATTER, 80, false, team);
        PlayerSeason otherSeason = ps("Twice", PlayerRole.BATTER, 82, false, team);
        otherSeason.setPlayer(first.getPlayer()); // same real player, different roster row (Dream XI scenario)
        assertThatThrownBy(() -> validator.validatePick(rules, List.of(first), otherSeason))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("cannot be picked twice");
    }

    @Test
    void enforcesOverseasLimit() {
        List<PlayerSeason> picked = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            picked.add(ps("Overseas " + i, PlayerRole.BATTER, 80, true, team));
        }
        PlayerSeason fifth = ps("Overseas 5", PlayerRole.BOWLER, 80, true, team);
        assertThatThrownBy(() -> validator.validatePick(rules, picked, fifth))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("Overseas limit");

        PlayerSeason domestic = ps("Home Bowler", PlayerRole.BOWLER, 80, false, team);
        assertThatCode(() -> validator.validatePick(rules, picked, domestic)).doesNotThrowAnyException();
    }

    @Test
    void rejectsPickWhenXiIsFull() {
        List<PlayerSeason> xi = Fixtures.idealXi(team, 75);
        PlayerSeason extra = ps("Twelfth", PlayerRole.BATTER, 70, false, team);
        assertThatThrownBy(() -> validator.validatePick(rules, xi, extra))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("already full");
    }

    @Test
    void refusesPickThatMakesRequiredRolesImpossible() {
        // 8 batters picked, then a 9th: only 2 slots would remain but we still need a WK and 3 bowlers.
        List<PlayerSeason> picked = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            picked.add(ps("Bat " + i, PlayerRole.BATTER, 70, false, team));
        }
        PlayerSeason ninth = ps("Bat 9", PlayerRole.BATTER, 70, false, team);
        assertThatThrownBy(() -> validator.validatePick(rules, picked, ninth))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("too few slots");
    }

    @Test
    void incompleteXiReportsEveryProblem() {
        List<PlayerSeason> picks = List.of(ps("Only Batter", PlayerRole.BATTER, 70, false, team));
        List<String> issues = validator.completionIssues(rules, picks);
        assertThat(issues).anyMatch(s -> s.contains("Select exactly 11"));
        assertThat(issues).anyMatch(s -> s.contains("wicketkeeper"));
        assertThat(issues).anyMatch(s -> s.contains("specialist bowler"));
    }

    @Test
    void validIdealXiHasNoIssues() {
        assertThat(validator.completionIssues(rules, Fixtures.idealXi(team, 80))).isEmpty();
    }

    @Test
    void invalidRoleCombinationIsReported() {
        // 11 players but no wicketkeeper and only 2 specialist bowlers
        List<PlayerSeason> xi = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            xi.add(ps("Bat " + i, PlayerRole.BATTER, 70, false, team));
        }
        xi.add(ps("Bowl A", PlayerRole.BOWLER, 70, false, team));
        xi.add(ps("Bowl B", PlayerRole.BOWLER, 70, false, team));
        List<String> issues = validator.completionIssues(rules, xi);
        assertThat(issues).anyMatch(s -> s.contains("1 more wicketkeeper"));
        assertThat(issues).anyMatch(s -> s.contains("1 more specialist bowler"));
    }

    @Test
    void challengeRulesRequireAllRounderAndCapOverseasAtThree() {
        SquadRules challenge = RuleSets.CHALLENGE;
        List<PlayerSeason> xi = Fixtures.idealXi(team, 70);
        xi.removeIf(p -> p.getRole() == PlayerRole.ALL_ROUNDER);
        xi.add(ps("Extra Bat 1", PlayerRole.BATTER, 70, false, team));
        xi.add(ps("Extra Bat 2", PlayerRole.BATTER, 70, false, team));
        assertThat(validator.completionIssues(challenge, xi)).anyMatch(s -> s.contains("all-rounder"));

        List<PlayerSeason> three = List.of(
                ps("O1", PlayerRole.BATTER, 60, true, team),
                ps("O2", PlayerRole.BATTER, 60, true, team),
                ps("O3", PlayerRole.BATTER, 60, true, team));
        PlayerSeason fourthOverseas = ps("O4", PlayerRole.BOWLER, 60, true, team);
        assertThatThrownBy(() -> validator.validatePick(challenge, three, fourthOverseas))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("at most 3 overseas");
    }

    @Test
    void budgetRuleRejectsPickThatExceedsTotalRating() {
        SquadRules budget = new SquadRules(11, 4, 1, 1, 3, 0, 200, null, null);
        List<PlayerSeason> picked = List.of(ps("A", PlayerRole.BATTER, 99, false, team),
                ps("B", PlayerRole.BATTER, 99, false, team));
        PlayerSeason c = ps("C", PlayerRole.BATTER, 50, false, team);
        assertThatThrownBy(() -> validator.validatePick(budget, picked, c))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("Budget exceeded");
    }

    @Test
    void maxFromOneFranchiseAndMaxPlayerRatingAreConfigurable() {
        SquadRules custom = new SquadRules(11, 4, 1, 1, 3, 0, null, 2, 90);
        PlayerSeason a = ps("A", PlayerRole.BATTER, 80, false, team);
        PlayerSeason b = ps("B", PlayerRole.BATTER, 80, false, team);
        PlayerSeason c = ps("C", PlayerRole.BATTER, 80, false, team);
        assertThatThrownBy(() -> validator.validatePick(custom, List.of(a, b), c))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("At most 2 players");

        PlayerSeason star = ps("Star", PlayerRole.BATTER, 95, false, Fixtures.team("OTH"));
        assertThatThrownBy(() -> validator.validatePick(custom, List.of(), star))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("rating cap");
    }

    @Test
    void poolCompositionDecidesDraftability() {
        PoolComposition ok = new PoolComposition(15, 1, 5, 4, 5, 5);
        assertThat(ok.isDraftable(RuleSets.STANDARD)).isTrue();
        assertThat(new PoolComposition(10, 1, 3, 3, 3, 0).isDraftable(RuleSets.STANDARD)).isFalse(); // < 11 players
        assertThat(new PoolComposition(14, 0, 5, 4, 5, 0).isDraftable(RuleSets.STANDARD)).isFalse(); // no keeper
        assertThat(new PoolComposition(14, 1, 5, 4, 4, 9).isDraftable(RuleSets.STANDARD)).isFalse(); // only 5 domestic
        assertThat(new PoolComposition(14, 1, 5, 0, 8, 0).isDraftable(RuleSets.CHALLENGE)).isFalse(); // no all-rounder
    }
}
