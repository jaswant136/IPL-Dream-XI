package com.ipldraftclub.controller;

import com.ipldraftclub.dto.DraftDtos.GameModeDto;
import com.ipldraftclub.entity.GameMode;
import com.ipldraftclub.game.RuleSets;
import com.ipldraftclub.mapper.DraftMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/game-modes")
public class GameModeController {

    private final DraftMapper mapper;

    public GameModeController(DraftMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public List<GameModeDto> modes() {
        return Arrays.stream(GameMode.values())
                .map(m -> new GameModeDto(m, m.label(), m.description(), m.hidesRatingsWhileDrafting(),
                        mapper.toRules(RuleSets.forMode(m))))
                .toList();
    }
}
