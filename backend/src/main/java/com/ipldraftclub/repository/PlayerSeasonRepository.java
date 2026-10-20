package com.ipldraftclub.repository;

import com.ipldraftclub.entity.PlayerSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason, UUID> {

    /** The exact, active roster of one franchise in one season. */
    @Query("""
            select ps from PlayerSeason ps
              join fetch ps.player join fetch ps.team
             where ps.team.id = :teamId and ps.season = :season and ps.active = true
             order by ps.rating desc, ps.player.fullName asc
            """)
    List<PlayerSeason> findPool(@Param("teamId") UUID teamId, @Param("season") int season);

    /** Every active roster entry of an active franchise (Dream XI pool). */
    @Query("""
            select ps from PlayerSeason ps
              join fetch ps.player join fetch ps.team
             where ps.active = true and ps.team.active = true
             order by ps.rating desc, ps.player.fullName asc
            """)
    List<PlayerSeason> findAllActive();

    @Query("""
            select ps from PlayerSeason ps
              join fetch ps.player join fetch ps.team
             where ps.id = :id
            """)
    Optional<PlayerSeason> findDetailedById(@Param("id") UUID id);

    boolean existsByPlayerIdAndTeamIdAndSeason(UUID playerId, UUID teamId, int season);

    Optional<PlayerSeason> findByPlayerIdAndTeamIdAndSeason(UUID playerId, UUID teamId, int season);

    /**
     * Roster composition grouped by franchise, season, role and overseas flag.
     * Rows: [teamId, season, role, overseas, count]. Used to decide which combinations are draftable.
     */
    @Query("""
            select ps.team.id, ps.season, ps.role, ps.overseas, count(ps)
              from PlayerSeason ps
             where ps.active = true and ps.team.active = true
             group by ps.team.id, ps.season, ps.role, ps.overseas
            """)
    List<Object[]> compositionByTeamSeason();

    long countByActiveTrue();
}
