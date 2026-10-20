package com.ipldraftclub.repository;

import com.ipldraftclub.entity.DraftPick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DraftPickRepository extends JpaRepository<DraftPick, UUID> {

    @Query("""
            select dp from DraftPick dp
              join fetch dp.playerSeason ps join fetch ps.player join fetch ps.team
             where dp.draft.id = :draftId
             order by dp.pickNumber asc
            """)
    List<DraftPick> findByDraft(@Param("draftId") UUID draftId);

    @Query("""
            select dp from DraftPick dp
              join fetch dp.playerSeason ps join fetch ps.player join fetch ps.team
             where dp.draft.id in :draftIds
             order by dp.draft.id asc, dp.pickNumber asc
            """)
    List<DraftPick> findByDraftIds(@Param("draftIds") Collection<UUID> draftIds);

    Optional<DraftPick> findByIdAndDraftId(UUID id, UUID draftId);

    @Modifying
    @Query("delete from DraftPick dp where dp.draft.id = :draftId")
    void deleteAllByDraftId(@Param("draftId") UUID draftId);
}
