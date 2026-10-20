package com.ipldraftclub.service;

import com.ipldraftclub.dto.TeamDtos.PlayerDto;
import com.ipldraftclub.dto.TeamDtos.PlayerPoolDto;
import com.ipldraftclub.dto.TeamDtos.TeamDto;
import com.ipldraftclub.dto.TeamDtos.TeamSeasonsDto;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;
import com.ipldraftclub.exception.ApiException;
import com.ipldraftclub.game.RuleSets;
import com.ipldraftclub.mapper.DraftMapper;
import com.ipldraftclub.repository.PlayerSeasonRepository;
import com.ipldraftclub.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teams;
    private final PlayerSeasonRepository playerSeasons;
    private final PoolCatalogService catalog;
    private final DraftMapper mapper;

    public TeamService(TeamRepository teams, PlayerSeasonRepository playerSeasons,
                       PoolCatalogService catalog, DraftMapper mapper) {
        this.teams = teams;
        this.playerSeasons = playerSeasons;
        this.catalog = catalog;
        this.mapper = mapper;
    }

    /** Every franchise, each with the seasons the backend can really draft (possibly none). */
    public List<TeamDto> listTeams() {
        Map<UUID, List<Integer>> seasons = catalog.draftableSeasonsByTeam(RuleSets.STANDARD);
        return teams.findAllByOrderByNameAsc().stream()
                .map(t -> new TeamDto(t.getId(), t.getCode(), t.getName(), t.isActive(),
                        seasons.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    public TeamSeasonsDto seasons(String teamCode) {
        Team team = requireTeam(teamCode);
        List<Integer> seasons = catalog.draftableSeasonsByTeam(RuleSets.STANDARD).getOrDefault(team.getId(), List.of());
        return new TeamSeasonsDto(team.getCode(), team.getName(), seasons);
    }

    /**
     * The exact roster of one franchise in one season. Ratings are intentionally omitted from this public
     * listing so it cannot be used to defeat Expert Mode; ratings are delivered inside draft responses.
     * An empty roster is a 404, never an empty "successful" list.
     */
    public PlayerPoolDto roster(String teamCode, int season) {
        Team team = requireTeam(teamCode);
        List<PlayerSeason> pool = playerSeasons.findPool(team.getId(), season);
        if (pool.isEmpty()) {
            throw ApiException.notFound("No roster data exists for " + team.getName() + " in the " + season
                    + " season.");
        }
        List<PlayerDto> players = pool.stream().map(ps -> mapper.toPlayer(ps, true)).toList();
        return new PlayerPoolDto(mapper.toTeamRef(team), season, players.size(), players);
    }

    private Team requireTeam(String code) {
        return teams.findByCodeIgnoreCase(code)
                .orElseThrow(() -> ApiException.notFound("Unknown franchise code '" + code + "'."));
    }
}
