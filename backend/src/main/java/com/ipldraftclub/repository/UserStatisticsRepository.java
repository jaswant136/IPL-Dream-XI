package com.ipldraftclub.repository;

import com.ipldraftclub.dto.LeaderboardEntryDto;
import com.ipldraftclub.entity.UserStatistics;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, UUID> {

    Optional<UserStatistics> findByFirebaseUid(String firebaseUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserStatistics s where s.firebaseUid = :uid")
    Optional<UserStatistics> findByFirebaseUidForUpdate(@Param("uid") String uid);

    String SELECT = """
            select new com.ipldraftclub.dto.LeaderboardEntryDto(0, p.displayName, s.completedDrafts,
                   s.averageRating, s.highestRating, s.points)
              from UserStatistics s, UserProfile p
             where p.firebaseUid = s.firebaseUid and s.completedDrafts > 0
            """;
    String COUNT = """
            select count(s) from UserStatistics s, UserProfile p
             where p.firebaseUid = s.firebaseUid and s.completedDrafts > 0
            """;

    // Every ordering ends with the same deterministic tie-breakers, so ties are always resolved identically.
    @Query(value = SELECT + " order by s.points desc, s.highestRating desc, s.completedDrafts desc, s.firebaseUid asc",
            countQuery = COUNT)
    Page<LeaderboardEntryDto> byPoints(Pageable pageable);

    @Query(value = SELECT + " order by s.averageRating desc, s.points desc, s.highestRating desc, s.firebaseUid asc",
            countQuery = COUNT)
    Page<LeaderboardEntryDto> byAverage(Pageable pageable);

    @Query(value = SELECT + " order by s.highestRating desc, s.points desc, s.completedDrafts desc, s.firebaseUid asc",
            countQuery = COUNT)
    Page<LeaderboardEntryDto> byHighest(Pageable pageable);

    @Query(value = SELECT + " order by s.completedDrafts desc, s.points desc, s.highestRating desc, s.firebaseUid asc",
            countQuery = COUNT)
    Page<LeaderboardEntryDto> byCompleted(Pageable pageable);

    /** Number of users ranked strictly ahead of the given user under the default (points) ordering. */
    @Query("""
            select count(s) from UserStatistics s
             where s.completedDrafts > 0 and (
                   s.points > :points
                or (s.points = :points and (s.highestRating > :highest
                or (s.highestRating = :highest and (s.completedDrafts > :completed
                or (s.completedDrafts = :completed and s.firebaseUid < :uid))))))
            """)
    long countRankedAhead(@Param("points") long points, @Param("highest") int highest,
                          @Param("completed") int completed, @Param("uid") String uid);
}
