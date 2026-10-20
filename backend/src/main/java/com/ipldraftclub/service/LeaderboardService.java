package com.ipldraftclub.service;

import com.ipldraftclub.dto.LeaderboardEntryDto;
import com.ipldraftclub.dto.PageResponse;
import com.ipldraftclub.exception.ApiException;
import com.ipldraftclub.repository.UserStatisticsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads persisted statistics, which are only ever written by the server when it completes a draft.
 * Client-supplied ratings never reach this code path.
 */
@Service
@Transactional(readOnly = true)
public class LeaderboardService {

    public static final int MAX_PAGE_SIZE = 50;

    private final UserStatisticsRepository stats;

    public LeaderboardService(UserStatisticsRepository stats) {
        this.stats = stats;
    }

    public PageResponse<LeaderboardEntryDto> page(int page, int size, String sort) {
        if (page < 0) {
            throw ApiException.badRequest("page must be 0 or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        PageRequest pageable = PageRequest.of(page, size);
        String key = sort == null ? "points" : sort.toLowerCase(Locale.ROOT);
        Page<LeaderboardEntryDto> result = switch (key) {
            case "points" -> stats.byPoints(pageable);
            case "average" -> stats.byAverage(pageable);
            case "highest" -> stats.byHighest(pageable);
            case "completed" -> stats.byCompleted(pageable);
            default -> throw ApiException.badRequest("sort must be one of: points, average, highest, completed.");
        };

        List<LeaderboardEntryDto> ranked = new ArrayList<>();
        int rank = page * size + 1;
        for (LeaderboardEntryDto row : result.getContent()) {
            ranked.add(row.withRank(rank++));
        }
        return new PageResponse<>(ranked, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
