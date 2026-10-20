package com.ipldraftclub.service;

import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.game.PoolComposition;
import com.ipldraftclub.game.SquadRules;
import com.ipldraftclub.repository.PlayerSeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The authority on which franchise-season combinations exist and are draftable.
 * Everything is derived from real roster rows - nothing is hard-coded - so a season that is not
 * in the database can never be offered or selected.
 */
@Service
@Transactional(readOnly = true)
public class PoolCatalogService {

    public record Combination(UUID teamId, int season) {
    }

    private final PlayerSeasonRepository playerSeasons;

    public PoolCatalogService(PlayerSeasonRepository playerSeasons) {
        this.playerSeasons = playerSeasons;
    }

    private Map<Combination, PoolComposition> compositions() {
        Map<Combination, PoolComposition> result = new HashMap<>();
        for (Object[] row : playerSeasons.compositionByTeamSeason()) {
            Combination key = new Combination((UUID) row[0], ((Number) row[1]).intValue());
            PlayerRole role = (PlayerRole) row[2];
            boolean overseas = (Boolean) row[3];
            int count = ((Number) row[4]).intValue();
            result.merge(key, PoolComposition.empty().plus(role, overseas, count),
                    (a, b) -> a.plus(role, overseas, count));
        }
        return result;
    }

    /** Franchise-season combinations from which a valid XI can be drafted under the rules. */
    public List<Combination> draftableCombinations(SquadRules rules) {
        List<Combination> out = new ArrayList<>();
        compositions().forEach((combo, comp) -> {
            if (comp.isDraftable(rules)) {
                out.add(combo);
            }
        });
        // stable ordering keeps behaviour deterministic for a given random index
        out.sort((a, b) -> a.teamId().equals(b.teamId())
                ? Integer.compare(a.season(), b.season())
                : a.teamId().toString().compareTo(b.teamId().toString()));
        return out;
    }

    /** Draftable seasons per franchise, ascending. Franchises without any appear nowhere in the map. */
    public Map<UUID, List<Integer>> draftableSeasonsByTeam(SquadRules rules) {
        Map<UUID, List<Integer>> map = new HashMap<>();
        for (Combination c : draftableCombinations(rules)) {
            map.computeIfAbsent(c.teamId(), k -> new ArrayList<>()).add(c.season());
        }
        map.values().forEach(list -> list.sort(Integer::compare));
        return map;
    }

    /**
     * True when the combined pool of all rosters can supply a valid Dream XI: enough players with the
     * required roles and domestic depth, AND enough franchises that a per-franchise cap still allows 11 players.
     */
    public boolean dreamPoolAvailable(SquadRules rules) {
        PoolComposition total = PoolComposition.empty();
        Map<UUID, Integer> perTeam = new HashMap<>();
        for (Map.Entry<Combination, PoolComposition> e : compositions().entrySet()) {
            PoolComposition c = e.getValue();
            perTeam.merge(e.getKey().teamId(), c.total(), Integer::sum);
            total = new PoolComposition(
                    total.total() + c.total(),
                    total.wicketkeepers() + c.wicketkeepers(),
                    total.batters() + c.batters(),
                    total.allRounders() + c.allRounders(),
                    total.bowlers() + c.bowlers(),
                    total.overseas() + c.overseas());
        }
        if (rules.maxFromOneFranchise() != null) {
            int cap = rules.maxFromOneFranchise();
            int usable = perTeam.values().stream().mapToInt(n -> Math.min(n, cap)).sum();
            if (usable < rules.squadSize()) {
                return false;
            }
        }
        return total.isDraftable(rules);
    }
}
