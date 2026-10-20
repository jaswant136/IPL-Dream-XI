package com.ipldraftclub.mapper;

import com.ipldraftclub.dto.DraftDtos.DraftDetailDto;
import com.ipldraftclub.dto.DraftDtos.DraftResultDto;
import com.ipldraftclub.dto.DraftDtos.DraftSummaryDto;
import com.ipldraftclub.dto.DraftDtos.PickDto;
import com.ipldraftclub.dto.DraftDtos.RulesDto;
import com.ipldraftclub.dto.DraftDtos.SharedDraftDto;
import com.ipldraftclub.dto.TeamDtos.PlayerDto;
import com.ipldraftclub.dto.TeamDtos.TeamRefDto;
import com.ipldraftclub.entity.Draft;
import com.ipldraftclub.entity.DraftPick;
import com.ipldraftclub.entity.GameMode;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;
import com.ipldraftclub.game.SquadRules;
import org.springframework.stereotype.Component;

import java.util.List;

/** Entity -> DTO conversion. This is also where hidden Expert Mode ratings are stripped. */
@Component
public class DraftMapper {

    /** Ratings are hidden for Expert Mode drafts until the server has completed them. */
    public boolean ratingsHidden(Draft draft) {
        return draft.getMode().hidesRatingsWhileDrafting() && !draft.isCompleted();
    }

    public PlayerDto toPlayer(PlayerSeason ps, boolean hideRating) {
        return new PlayerDto(
                ps.getId(),
                ps.getPlayer().getId(),
                ps.getPlayer().getFullName(),
                ps.getPlayer().getCountry(),
                ps.getRole(),
                ps.isOverseas(),
                hideRating ? null : ps.getRating(),
                ps.getPlayer().getBattingStyle(),
                ps.getPlayer().getBowlingStyle(),
                ps.getTeam().getCode(),
                ps.getTeam().getName(),
                ps.getSeason());
    }

    public TeamRefDto toTeamRef(Team team) {
        return team == null ? null : new TeamRefDto(team.getId(), team.getCode(), team.getName());
    }

    public RulesDto toRules(SquadRules r) {
        return new RulesDto(r.squadSize(), r.maxOverseas(), r.minWicketkeepers(), r.minBatters(), r.minBowlers(),
                r.minAllRounders(), r.maxTotalRating(), r.maxFromOneFranchise(), r.maxPlayerRating());
    }

    public List<PickDto> toPicks(List<DraftPick> picks, boolean hideRating) {
        return picks.stream()
                .map(p -> new PickDto(p.getId(), p.getPickNumber(), toPlayer(p.getPlayerSeason(), hideRating)))
                .toList();
    }

    public DraftDetailDto toDetail(Draft d, List<DraftPick> picks, List<PlayerSeason> pool, SquadRules rules,
                                   List<String> issues) {
        boolean hide = ratingsHidden(d);
        int overseas = (int) picks.stream().filter(p -> p.getPlayerSeason().isOverseas()).count();
        return new DraftDetailDto(
                d.getId(),
                d.getMode(),
                d.getMode().label(),
                toTeamRef(d.getTeam()),
                d.getSeason(),
                d.getMode() == GameMode.DREAM,
                d.getStatus(),
                d.getChallengeCode(),
                toRules(rules),
                hide,
                d.getStartedAt(),
                d.getCompletedAt(),
                picks.size(),
                rules.squadSize(),
                overseas,
                issues.isEmpty(),
                issues,
                toPicks(picks, hide),
                pool == null ? null : pool.stream().map(ps -> toPlayer(ps, hide)).toList(),
                d.isShared(),
                d.isShared() ? d.getShareToken() : null);
    }

    public DraftSummaryDto toSummary(Draft d, List<DraftPick> picks) {
        return new DraftSummaryDto(
                d.getId(),
                d.getMode(),
                d.getMode().label(),
                toTeamRef(d.getTeam()),
                d.getSeason(),
                d.getStatus(),
                d.getChallengeCode(),
                d.getStartedAt(),
                d.getCompletedAt(),
                picks.size(),
                d.getTeamRating(),
                d.getPoints(),
                d.isShared(),
                picks.stream().map(p -> p.getPlayerSeason().getPlayer().getFullName()).toList());
    }

    public DraftResultDto toResult(Draft d, List<DraftPick> picks) {
        return new DraftResultDto(
                d.getId(),
                d.getMode(),
                d.getMode().label(),
                toTeamRef(d.getTeam()),
                d.getSeason(),
                d.getChallengeCode(),
                d.getTeamRating(),
                d.getBattingScore(),
                d.getBowlingScore(),
                d.getAllRoundScore(),
                d.getWicketkeepingScore(),
                d.getRoleBalanceScore(),
                d.getOverseasCount(),
                d.getPoints(),
                d.getCompletionSeconds(),
                true,
                List.of(),
                d.getCompletedAt(),
                toPicks(picks, false),
                d.isShared(),
                d.isShared() ? d.getShareToken() : null);
    }

    public SharedDraftDto toShared(Draft d, List<DraftPick> picks) {
        return new SharedDraftDto(
                d.getMode(),
                d.getMode().label(),
                toTeamRef(d.getTeam()),
                d.getSeason(),
                d.getChallengeCode(),
                d.getTeamRating(),
                d.getBattingScore(),
                d.getBowlingScore(),
                d.getAllRoundScore(),
                d.getWicketkeepingScore(),
                d.getRoleBalanceScore(),
                d.getOverseasCount(),
                d.getCompletedAt(),
                toPicks(picks, false));
    }
}
