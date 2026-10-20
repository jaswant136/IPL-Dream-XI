package com.ipldraftclub.controller;

import com.ipldraftclub.dto.UserDtos.MeDto;
import com.ipldraftclub.dto.UserDtos.UpdateProfileRequest;
import com.ipldraftclub.dto.UserDtos.UserStatisticsDto;
import com.ipldraftclub.security.AuthenticatedUser;
import com.ipldraftclub.service.StatisticsService;
import com.ipldraftclub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserService users;
    private final StatisticsService statistics;

    public MeController(UserService users, StatisticsService statistics) {
        this.users = users;
        this.statistics = statistics;
    }

    @GetMapping
    public MeDto me(@AuthenticationPrincipal AuthenticatedUser user) {
        return users.getMe(user);
    }

    @PutMapping("/profile")
    public MeDto updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return users.updateDisplayName(user, request.displayName());
    }

    @GetMapping("/statistics")
    public UserStatisticsDto statistics(@AuthenticationPrincipal AuthenticatedUser user) {
        return statistics.forUser(user.uid());
    }
}
