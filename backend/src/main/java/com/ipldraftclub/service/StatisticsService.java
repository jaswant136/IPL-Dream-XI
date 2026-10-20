package com.ipldraftclub.service;

import com.ipldraftclub.dto.DraftDtos.DraftSummaryDto;
import com.ipldraftclub.dto.UserDtos.UserStatisticsDto;
import com.ipldraftclub.entity.DraftStatus;
import com.ipldraftclub.entity.UserStatistics;
import com.ipldraftclub.mapper.DraftMapper;
import com.ipldraftclub.repository.DraftPickRepository;
import com.ipldraftclub.repository.DraftRepository;
import com.ipldraftclub.repository.UserStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final UserStatisticsRepository stats;
    private final DraftRepository drafts;
    private final DraftPickRepository picks;
    private final DraftMapper mapper;

    public StatisticsService(UserStatisticsRepository stats, DraftRepository drafts,
                             DraftPickRepository picks, DraftMapper mapper) {
        this.stats = stats;
        this.drafts = drafts;
        this.picks = picks;
        this.mapper = mapper;
    }

    /** Real persisted numbers only; a brand-new user simply gets zeros and no best draft. */
    public UserStatisticsDto forUser(String uid) {
        UserStatistics s = stats.findByFirebaseUid(uid).orElseGet(() -> new UserStatistics(uid));

        Integer position = null;
        if (s.getCompletedDrafts() > 0) {
            position = (int) stats.countRankedAhead(s.getPoints(), s.getHighestRating(),
                    s.getCompletedDrafts(), uid) + 1;
        }

        DraftSummaryDto best = drafts.findFirstByFirebaseUidAndStatusOrderByTeamRatingDescCompletedAtAsc(uid, DraftStatus.COMPLETED)
                .map(d -> mapper.toSummary(d, picks.findByDraft(d.getId())))
                .orElse(null);

        return new UserStatisticsDto(s.getTotalDrafts(), s.getCompletedDrafts(), s.getAverageRating(),
                s.getHighestRating(), s.getPoints(), position, best);
    }
}
