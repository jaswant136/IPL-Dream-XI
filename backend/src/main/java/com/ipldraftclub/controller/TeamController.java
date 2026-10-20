package com.ipldraftclub.controller;

import com.ipldraftclub.dto.TeamDtos.PlayerPoolDto;
import com.ipldraftclub.dto.TeamDtos.TeamDto;
import com.ipldraftclub.dto.TeamDtos.TeamSeasonsDto;
import com.ipldraftclub.service.TeamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<TeamDto> teams() {
        return teamService.listTeams();
    }

    @GetMapping("/{teamCode}/seasons")
    public TeamSeasonsDto seasons(@PathVariable String teamCode) {
        return teamService.seasons(teamCode);
    }

    @GetMapping("/{teamCode}/seasons/{season}/players")
    public PlayerPoolDto players(@PathVariable String teamCode, @PathVariable int season) {
        return teamService.roster(teamCode, season);
    }
}
