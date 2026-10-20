package com.ipldraftclub.controller;

import com.ipldraftclub.dto.LeaderboardEntryDto;
import com.ipldraftclub.dto.PageResponse;
import com.ipldraftclub.service.LeaderboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboard;

    public LeaderboardController(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    /** sort = points (default) | average | highest | completed */
    @GetMapping
    public PageResponse<LeaderboardEntryDto> leaderboard(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size,
                                                         @RequestParam(defaultValue = "points") String sort) {
        return leaderboard.page(page, size, sort);
    }
}
