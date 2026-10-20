package com.ipldraftclub.service;

import com.ipldraftclub.config.AppProperties;
import com.ipldraftclub.dto.DraftDtos.DraftDetailDto;
import com.ipldraftclub.dto.DraftDtos.DraftResultDto;
import com.ipldraftclub.dto.DraftDtos.DraftSummaryDto;
import com.ipldraftclub.dto.DraftDtos.ShareDto;
import com.ipldraftclub.dto.DraftDtos.SharedDraftDto;
import com.ipldraftclub.dto.PageResponse;
import com.ipldraftclub.entity.Draft;
import com.ipldraftclub.entity.DraftPick;
import com.ipldraftclub.entity.DraftStatus;
import com.ipldraftclub.entity.GameMode;
import com.ipldraftclub.entity.PlayerSeason;
import com.ipldraftclub.entity.Team;
import com.ipldraftclub.entity.UserStatistics;
import com.ipldraftclub.exception.ApiException;
import com.ipldraftclub.exception.NoEligiblePoolException;
import com.ipldraftclub.exception.RuleViolationException;
import com.ipldraftclub.game.RuleSets;
import com.ipldraftclub.game.SquadRules;
import com.ipldraftclub.game.SquadValidator;
import com.ipldraftclub.game.TeamScore;
import com.ipldraftclub.game.TeamScorer;
import com.ipldraftclub.mapper.DraftMapper;
import com.ipldraftclub.repository.DraftPickRepository;
import com.ipldraftclub.repository.DraftRepository;
import com.ipldraftclub.repository.PlayerSeasonRepository;
import com.ipldraftclub.repository.TeamRepository;
import com.ipldraftclub.repository.UserStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

/**
 * The draft lifecycle: start -> add/remove picks -> complete. Every operation takes the authenticated UID
 * from the verified token and scopes all lookups by it, so another user's draft is indistinguishable
 * from a non-existent one (404). Mutations run in one transaction under a row lock on the draft.
 */
@Service
public class DraftService {

    private static final Logger log = LoggerFactory.getLogger(DraftService.class);
    private static final Pattern SHARE_TOKEN = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");
    private static final String CHALLENGE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public record StartResult(DraftDetailDto draft, boolean created) {
    }

    private final DraftRepository drafts;
    private final DraftPickRepository picks;
    private final PlayerSeasonRepository playerSeasons;
    private final TeamRepository teams;
    private final UserStatisticsRepository stats;
    private final PoolCatalogService catalog;
    private final SquadValidator validator;
    private final TeamScorer scorer;
    private final DraftMapper mapper;
    private final RateLimiter rateLimiter;
    private final AppProperties props;
    private final RandomGenerator random;
    private final SecureRandom secureRandom = new SecureRandom();

    public DraftService(DraftRepository drafts, DraftPickRepository picks, PlayerSeasonRepository playerSeasons,
                        TeamRepository teams, UserStatisticsRepository stats, PoolCatalogService catalog,
                        SquadValidator validator, TeamScorer scorer, DraftMapper mapper, RateLimiter rateLimiter,
                        AppProperties props, RandomGenerator random) {
        this.drafts = drafts;
        this.picks = picks;
        this.playerSeasons = playerSeasons;
        this.teams = teams;
        this.stats = stats;
        this.catalog = catalog;
        this.validator = validator;
        this.scorer = scorer;
        this.mapper = mapper;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.random = random;
    }

    // ------------------------------------------------------------------ start

    @Transactional
    public StartResult start(String uid, GameMode mode, String idempotencyKey) {
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > 64)) {
            throw ApiException.badRequest("Idempotency-Key must be 1-64 characters.");
        }
        rateLimiter.check("start:" + uid, props.rateLimit().startDraftPerMinute());

        if (idempotencyKey != null) {
            Optional<Draft> existing = drafts.findByFirebaseUidAndIdempotencyKey(uid, idempotencyKey);
            if (existing.isPresent()) {
                return new StartResult(detail(existing.get()), false);
            }
        }

        SquadRules rules = RuleSets.forMode(mode);
        Team team = null;
        Integer season = null;
        List<PlayerSeason> pool;

        if (mode == GameMode.DREAM) {
            if (!catalog.dreamPoolAvailable(rules)) {
                throw new NoEligiblePoolException("Dream XI is not available: the roster database does not yet "
                        + "contain enough players to build a valid XI.");
            }
            pool = playerSeasons.findAllActive();
        } else {
            // The BACKEND picks the combination, from real draftable roster data only.
            List<PoolCatalogService.Combination> combos = catalog.draftableCombinations(rules);
            if (combos.isEmpty()) {
                throw new NoEligiblePoolException("No franchise-season roster is currently draftable for "
                        + mode.label() + ". Import roster data first.");
            }
            PoolCatalogService.Combination chosen = combos.get(random.nextInt(combos.size()));
            team = teams.findById(chosen.teamId())
                    .orElseThrow(() -> new NoEligiblePoolException("The selected franchise no longer exists."));
            season = chosen.season();
            pool = playerSeasons.findPool(team.getId(), season);
        }

        if (pool.isEmpty()) {
            // Defensive: never create a draft with an empty pool.
            throw new NoEligiblePoolException("The selected roster has no eligible players.");
        }

        Instant now = Instant.now();
        Draft draft = new Draft();
        draft.setFirebaseUid(uid);
        draft.setMode(mode);
        draft.setTeam(team);
        draft.setSeason(season);
        draft.setStatus(DraftStatus.IN_PROGRESS);
        draft.setIdempotencyKey(idempotencyKey);
        draft.setChallengeCode(mode == GameMode.CHALLENGE ? newChallengeCode() : null);
        draft.setStartedAt(now);
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        drafts.saveAndFlush(draft);

        UserStatistics s = stats.findByFirebaseUidForUpdate(uid).orElseGet(() -> stats.save(new UserStatistics(uid)));
        s.recordStarted();
        stats.save(s);

        log.info("Draft {} started in mode {} ({} {})", draft.getId(), mode,
                team == null ? "multi-franchise" : team.getCode(), season == null ? "multi-season" : season);

        List<String> issues = validator.completionIssues(rules, List.of());
        return new StartResult(mapper.toDetail(draft, List.of(), pool, rules, issues), true);
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public DraftDetailDto get(String uid, UUID draftId) {
        return detail(requireOwned(uid, draftId));
    }

    @Transactional(readOnly = true)
    public PageResponse<DraftSummaryDto> list(String uid, int page, int size, DraftStatus status) {
        if (page < 0) {
            throw ApiException.badRequest("page must be 0 or greater.");
        }
        if (size < 1 || size > 50) {
            throw ApiException.badRequest("size must be between 1 and 50.");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Draft> result = status == null
                ? drafts.findByFirebaseUid(uid, pageable)
                : drafts.findByFirebaseUidAndStatus(uid, status, pageable);

        Map<UUID, List<DraftPick>> picksByDraft = new HashMap<>();
        if (!result.isEmpty()) {
            List<UUID> ids = result.getContent().stream().map(Draft::getId).toList();
            for (DraftPick p : picks.findByDraftIds(ids)) {
                picksByDraft.computeIfAbsent(p.getDraft().getId(), k -> new java.util.ArrayList<>()).add(p);
            }
        }
        return PageResponse.of(result, d -> mapper.toSummary(d, picksByDraft.getOrDefault(d.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public DraftResultDto result(String uid, UUID draftId) {
        Draft d = requireOwned(uid, draftId);
        if (!d.isCompleted()) {
            throw ApiException.conflict("This draft has not been completed yet, so there is no result.");
        }
        return mapper.toResult(d, picks.findByDraft(d.getId()));
    }

    // ------------------------------------------------------------------ add pick

    @Transactional
    public DraftDetailDto addPick(String uid, UUID draftId, UUID playerSeasonId) {
        Draft d = requireOwnedForUpdate(uid, draftId);
        requireInProgress(d);
        SquadRules rules = RuleSets.forMode(d.getMode());

        PlayerSeason candidate = playerSeasons.findDetailedById(playerSeasonId).orElse(null);
        if (candidate == null || !isEligible(d, candidate)) {
            throw new RuleViolationException("The selected player is not eligible for this draft.");
        }

        List<DraftPick> current = picks.findByDraft(d.getId());
        List<PlayerSeason> chosen = current.stream().map(DraftPick::getPlayerSeason).toList();
        validator.validatePick(rules, chosen, candidate);

        picks.saveAndFlush(new DraftPick(d, candidate, current.size() + 1));
        touch(d);
        return detailWithoutPool(d, rules);
    }

    // ------------------------------------------------------------------ remove pick

    @Transactional
    public DraftDetailDto removePick(String uid, UUID draftId, UUID pickId) {
        Draft d = requireOwnedForUpdate(uid, draftId);
        requireInProgress(d);
        SquadRules rules = RuleSets.forMode(d.getMode());

        DraftPick pick = picks.findByIdAndDraftId(pickId, d.getId())
                .orElseThrow(() -> ApiException.notFound("That pick does not exist in this draft."));
        int removedNumber = pick.getPickNumber();
        picks.delete(pick);
        picks.flush();

        // Close the gap so pick numbers stay 1..n. Ascending order + flush keeps the unique constraint satisfied.
        for (DraftPick later : picks.findByDraft(d.getId())) {
            if (later.getPickNumber() > removedNumber) {
                later.setPickNumber(later.getPickNumber() - 1);
                picks.saveAndFlush(later);
            }
        }
        touch(d);
        return detailWithoutPool(d, rules);
    }

    // ------------------------------------------------------------------ complete

    @Transactional
    public DraftResultDto complete(String uid, UUID draftId) {
        Draft d = requireOwnedForUpdate(uid, draftId);
        requireInProgress(d);
        SquadRules rules = RuleSets.forMode(d.getMode());

        List<DraftPick> current = picks.findByDraft(d.getId());
        List<PlayerSeason> chosen = current.stream().map(DraftPick::getPlayerSeason).toList();

        List<String> issues = validator.completionIssues(rules, chosen);
        if (!issues.isEmpty()) {
            throw new RuleViolationException("Your XI cannot be completed yet: " + String.join(" ", issues), issues);
        }

        TeamScore score = scorer.score(rules, chosen);
        Instant now = Instant.now();
        int points = (int) Math.round(score.teamRating() * RuleSets.pointsMultiplier(d.getMode()));

        d.setStatus(DraftStatus.COMPLETED);
        d.setCompletedAt(now);
        d.setCompletionSeconds(Math.max(0, Duration.between(d.getStartedAt(), now).getSeconds()));
        d.setTeamRating(score.teamRating());
        d.setTotalScore(score.teamRating());
        d.setPoints(points);
        d.setBattingScore(score.battingScore());
        d.setBowlingScore(score.bowlingScore());
        d.setAllRoundScore(score.allRoundScore());
        d.setWicketkeepingScore(score.wicketkeepingScore());
        d.setRoleBalanceScore(score.roleBalanceScore());
        d.setOverseasCount(score.overseasPlayers());
        d.setUpdatedAt(now);
        drafts.saveAndFlush(d);

        UserStatistics s = stats.findByFirebaseUidForUpdate(uid).orElseGet(() -> stats.save(new UserStatistics(uid)));
        s.recordCompleted(score.teamRating(), points);
        stats.save(s);

        log.info("Draft {} completed: rating {}, points {}", d.getId(), score.teamRating(), points);
        return mapper.toResult(d, current);
    }

    // ------------------------------------------------------------------ delete

    @Transactional
    public void delete(String uid, UUID draftId) {
        Draft d = requireOwnedForUpdate(uid, draftId);
        if (d.isCompleted()) {
            throw ApiException.conflict("Completed drafts are permanent and cannot be deleted.");
        }
        picks.deleteAllByDraftId(d.getId());
        drafts.delete(d);
        drafts.flush();
    }

    // ------------------------------------------------------------------ sharing

    @Transactional
    public ShareDto share(String uid, UUID draftId) {
        Draft d = requireOwnedForUpdate(uid, draftId);
        if (!d.isCompleted()) {
            throw ApiException.conflict("Only completed drafts can be shared.");
        }
        if (!d.isShared() || d.getShareToken() == null) {
            byte[] bytes = new byte[24];
            secureRandom.nextBytes(bytes);
            d.setShareToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
            d.setShared(true);
            touch(d);
        }
        return new ShareDto(d.getShareToken(), "/shared/" + d.getShareToken());
    }

    @Transactional
    public void revokeShare(String uid, UUID draftId) {
        Draft d = requireOwnedForUpdate(uid, draftId);
        d.setShared(false);
        d.setShareToken(null);
        touch(d);
    }

    @Transactional(readOnly = true)
    public SharedDraftDto shared(String token) {
        if (token == null || !SHARE_TOKEN.matcher(token).matches()) {
            throw ApiException.notFound("This shared draft does not exist or is no longer shared.");
        }
        Draft d = drafts.findByShareTokenAndSharedTrue(token)
                .filter(Draft::isCompleted)
                .orElseThrow(() -> ApiException.notFound("This shared draft does not exist or is no longer shared."));
        return mapper.toShared(d, picks.findByDraft(d.getId()));
    }

    // ------------------------------------------------------------------ helpers

    private Draft requireOwned(String uid, UUID draftId) {
        return drafts.findByIdAndFirebaseUid(draftId, uid)
                .orElseThrow(() -> ApiException.notFound("Draft not found."));
    }

    private Draft requireOwnedForUpdate(String uid, UUID draftId) {
        return drafts.findOwnedForUpdate(draftId, uid)
                .orElseThrow(() -> ApiException.notFound("Draft not found."));
    }

    private void requireInProgress(Draft d) {
        if (d.isCompleted()) {
            throw ApiException.conflict("This draft is completed and can no longer be changed. Start a new draft instead.");
        }
    }

    private boolean isEligible(Draft d, PlayerSeason ps) {
        if (!ps.isActive() || !ps.getTeam().isActive()) {
            return false;
        }
        if (d.getMode() == GameMode.DREAM) {
            return true;
        }
        return d.getTeam() != null && d.getSeason() != null
                && ps.getTeam().getId().equals(d.getTeam().getId())
                && ps.getSeason() == d.getSeason();
    }

    private void touch(Draft d) {
        d.setUpdatedAt(Instant.now());
        drafts.saveAndFlush(d);
    }

    private DraftDetailDto detail(Draft d) {
        SquadRules rules = RuleSets.forMode(d.getMode());
        List<DraftPick> current = picks.findByDraft(d.getId());
        List<PlayerSeason> chosen = current.stream().map(DraftPick::getPlayerSeason).toList();
        List<String> issues = d.isCompleted() ? List.of() : validator.completionIssues(rules, chosen);

        List<PlayerSeason> pool;
        if (d.isCompleted()) {
            pool = List.of();
        } else if (d.getMode() == GameMode.DREAM) {
            pool = playerSeasons.findAllActive();
        } else {
            pool = playerSeasons.findPool(d.getTeam().getId(), d.getSeason());
        }
        return mapper.toDetail(d, current, pool, rules, issues);
    }

    private DraftDetailDto detailWithoutPool(Draft d, SquadRules rules) {
        List<DraftPick> current = picks.findByDraft(d.getId());
        List<PlayerSeason> chosen = current.stream().map(DraftPick::getPlayerSeason).toList();
        return mapper.toDetail(d, current, null, rules, validator.completionIssues(rules, chosen));
    }

    private String newChallengeCode() {
        StringBuilder sb = new StringBuilder("CH-");
        for (int i = 0; i < 8; i++) {
            sb.append(CHALLENGE_ALPHABET.charAt(secureRandom.nextInt(CHALLENGE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
