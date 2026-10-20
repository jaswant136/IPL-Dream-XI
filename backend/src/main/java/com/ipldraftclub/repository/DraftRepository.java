package com.ipldraftclub.repository;

import com.ipldraftclub.entity.Draft;
import com.ipldraftclub.entity.DraftStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DraftRepository extends JpaRepository<Draft, UUID> {

    @EntityGraph(attributePaths = "team")
    Optional<Draft> findByIdAndFirebaseUid(UUID id, String firebaseUid);

    /** Row-locks the draft so concurrent mutations of the same draft are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Draft d left join fetch d.team where d.id = :id and d.firebaseUid = :uid")
    Optional<Draft> findOwnedForUpdate(@Param("id") UUID id, @Param("uid") String uid);

    @EntityGraph(attributePaths = "team")
    Optional<Draft> findByFirebaseUidAndIdempotencyKey(String firebaseUid, String idempotencyKey);

    @EntityGraph(attributePaths = "team")
    Optional<Draft> findByShareTokenAndSharedTrue(String shareToken);

    @EntityGraph(attributePaths = "team")
    Page<Draft> findByFirebaseUid(String firebaseUid, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<Draft> findByFirebaseUidAndStatus(String firebaseUid, DraftStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Optional<Draft> findFirstByFirebaseUidAndStatusOrderByTeamRatingDescCompletedAtAsc(String firebaseUid, DraftStatus status);

    long countByFirebaseUidAndStatus(String firebaseUid, DraftStatus status);
}
