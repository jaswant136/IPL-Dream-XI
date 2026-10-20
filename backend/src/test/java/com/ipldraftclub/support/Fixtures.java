package com.ipldraftclub.support;

import com.ipldraftclub.entity.Player;
import com.ipldraftclub.entity.PlayerRole;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** In-memory entity builders for pure unit tests (no database). */
public final class Fixtures {

    private Fixtures() {
    }

    public static Team team(String code) {
        Team t = new Team(code, code + " Test Franchise", true);
        t.setId(UUID.randomUUID());
        return t;
    }

    public static PlayerSeason ps(String name, PlayerRole role, int rating, boolean overseas, Team team) {
        Player p = new Player(name, overseas ? "Australia" : "India", null, null, role);
        p.setId(UUID.randomUUID());
        PlayerSeason ps = new PlayerSeason(p, team, 2023, role, rating, overseas);
        ps.setId(UUID.randomUUID());
        return ps;
    }

    /** 1 WK, 4 BATTER, 2 ALL_ROUNDER, 4 BOWLER, all domestic, same rating. */
    public static List<PlayerSeason> idealXi(Team team, int rating) {
        List<PlayerSeason> xi = new ArrayList<>();
        xi.add(ps("WK One", PlayerRole.WICKETKEEPER, rating, false, team));
        for (int i = 1; i <= 4; i++) {
            xi.add(ps("Batter " + i, PlayerRole.BATTER, rating, false, team));
        }
        for (int i = 1; i <= 2; i++) {
            xi.add(ps("AllRounder " + i, PlayerRole.ALL_ROUNDER, rating, false, team));
        }
        for (int i = 1; i <= 4; i++) {
            xi.add(ps("Bowler " + i, PlayerRole.BOWLER, rating, false, team));
        }
        return xi;
    }
}
