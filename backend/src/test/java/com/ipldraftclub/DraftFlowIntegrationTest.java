package com.ipldraftclub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipldraftclub.seed.ImportSummary;
import com.ipldraftclub.seed.RosterCsvImporter;
import com.ipldraftclub.support.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API tests over the real Spring Security chain, Flyway migrations and JPA (H2 in PostgreSQL mode).
 * Authentication uses the test-only verifier: "Bearer test-&lt;uid&gt;".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DraftFlowIntegrationTest {

    /** CSK 2023: 14 players, draftable. MI 2022 (3) and RCB 2023 (6) are too thin to draft alone but feed the Dream pool. */
    static final String CSV = """
            player_name,country,team_code,season,role,rating
            Keeper One,India,CSK,2023,WICKETKEEPER,70
            Bat One,India,CSK,2023,BATTER,80
            Bat Two,India,CSK,2023,BATTER,78
            Bat Three,New Zealand,CSK,2023,BATTER,84
            AR One,India,CSK,2023,ALL_ROUNDER,82
            AR Two,England,CSK,2023,ALL_ROUNDER,79
            Bowl One,India,CSK,2023,BOWLER,75
            Bowl Two,India,CSK,2023,BOWLER,74
            Bowl Three,Sri Lanka,CSK,2023,BOWLER,77
            Bowl Four,India,CSK,2023,BOWLER,66
            Bowl Five,India,CSK,2023,BOWLER,61
            Bat Four,India,CSK,2023,BATTER,64
            Bat Five,Australia,CSK,2023,BATTER,69
            Bat Six,England,CSK,2023,BATTER,71
            Thin Roster One,India,MI,2022,BATTER,70
            Thin Roster Two,India,MI,2022,BOWLER,70
            Thin Roster Three,India,MI,2022,WICKETKEEPER,70
            Other Club One,India,RCB,2023,BATTER,70
            Other Club Two,India,RCB,2023,BATTER,70
            Other Club Three,India,RCB,2023,BOWLER,70
            Other Club Four,India,RCB,2023,BOWLER,70
            Other Club Five,India,RCB,2023,ALL_ROUNDER,70
            Other Club Six,India,RCB,2023,WICKETKEEPER,70
            """;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RosterCsvImporter importer;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() throws Exception {
        jdbc.update("delete from draft_picks");
        jdbc.update("delete from drafts");
        jdbc.update("delete from user_statistics");
        jdbc.update("delete from user_profiles");
        jdbc.update("delete from player_seasons");
        jdbc.update("delete from players");
        ImportSummary summary = importer.importCsv(new StringReader(CSV), "test", true);
        assertThat(summary.invalid()).isZero();
    }

    private String auth(String uid) {
        return "Bearer test-" + uid;
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode startDraft(String uid, String mode) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/drafts/start").header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"" + mode + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return body(r);
    }

    private JsonNode pickByName(JsonNode draft, String name) {
        for (JsonNode p : draft.get("players")) {
            if (p.get("name").asText().equals(name)) {
                return p;
            }
        }
        throw new AssertionError("player not in pool: " + name);
    }

    private JsonNode add(String uid, String draftId, String playerSeasonId, int expectedStatus) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/drafts/" + draftId + "/picks").header("Authorization", auth(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerSeasonId\":\"" + playerSeasonId + "\"}"))
                .andExpect(status().is(expectedStatus)).andReturn();
        return body(r);
    }

    private List<String> validXiNames() {
        // WK, 4 batters, 2 all-rounders, 4 bowlers; overseas: Bat Three (NZ), AR Two (ENG), Bowl Three (SL) = 3
        return List.of("Keeper One", "Bat One", "Bat Two", "Bat Three", "Bat Four",
                "AR One", "AR Two", "Bowl One", "Bowl Two", "Bowl Three", "Bowl Four");
    }

    // ------------------------------------------------------------ public endpoints

    @Test
    void publicEndpointsWorkWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/v1/teams")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/game-modes")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/v1/leaderboard")).andExpect(status().isOk());
    }

    @Test
    void onlySeededDraftableSeasonsAreOffered() throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/teams")).andExpect(status().isOk()).andReturn();
        for (JsonNode t : body(r)) {
            if (t.get("code").asText().equals("CSK")) {
                assertThat(t.get("seasons")).hasSize(1);
                assertThat(t.get("seasons").get(0).asInt()).isEqualTo(2023);
            }
            if (t.get("code").asText().equals("MI")) {
                assertThat(t.get("seasons")).isEmpty(); // 2022 exists but is too thin to draft
            }
        }
    }

    @Test
    void playerPoolEndpointReturnsExactRosterWithoutRatings() throws Exception {
        mvc.perform(get("/api/v1/teams/CSK/seasons/2023/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerCount").value(14))
                .andExpect(jsonPath("$.players[0].rating").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void emptyRosterReturns404NotAnEmptyList() throws Exception {
        mvc.perform(get("/api/v1/teams/CSK/seasons/2024/players")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/teams/ZZZ/seasons/2023/players")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ authentication

    @Test
    void protectedEndpointsRejectMissingAndInvalidTokens() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/drafts")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/drafts/start").contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CLASSIC\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").header("Authorization", auth("alice"))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------ spin / start

    @Test
    void startAlwaysChoosesTheOnlyDraftableCombination() throws Exception {
        for (int i = 0; i < 5; i++) {
            JsonNode d = startDraft("alice", "CLASSIC");
            assertThat(d.get("team").get("code").asText()).isEqualTo("CSK");
            assertThat(d.get("season").asInt()).isEqualTo(2023);
            assertThat(d.get("status").asText()).isEqualTo("IN_PROGRESS");
            assertThat(d.get("players")).hasSize(14);
        }
    }

    @Test
    void startFailsClearlyWhenNoRosterIsDraftable() throws Exception {
        jdbc.update("delete from player_seasons");
        mvc.perform(post("/api/v1/drafts/start").header("Authorization", auth("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"CLASSIC\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No franchise-season roster")));
    }

    @Test
    void startRejectsInvalidMode() throws Exception {
        mvc.perform(post("/api/v1/drafts/start").header("Authorization", auth("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/drafts/start").header("Authorization", auth("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idempotencyKeyReturnsTheSameDraft() throws Exception {
        MvcResult first = mvc.perform(post("/api/v1/drafts/start").header("Authorization", auth("alice"))
                        .header("Idempotency-Key", "abc-123").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CLASSIC\"}"))
                .andExpect(status().isCreated()).andReturn();
        MvcResult second = mvc.perform(post("/api/v1/drafts/start").header("Authorization", auth("alice"))
                        .header("Idempotency-Key", "abc-123").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CLASSIC\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(body(second).get("draftId").asText()).isEqualTo(body(first).get("draftId").asText());
    }

    // ------------------------------------------------------------ picks

    @Test
    void addAndRemovePicksKeepsOrderingAndRejectsDuplicates() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();

        JsonNode afterOne = add("alice", id, pickByName(d, "Keeper One").get("id").asText(), 201);
        assertThat(afterOne.get("pickCount").asInt()).isEqualTo(1);
        add("alice", id, pickByName(d, "Keeper One").get("id").asText(), 422); // duplicate
        JsonNode afterTwo = add("alice", id, pickByName(d, "Bat One").get("id").asText(), 201);
        add("alice", id, pickByName(d, "Bat Two").get("id").asText(), 201);

        String firstPickId = afterTwo.get("picks").get(0).get("pickId").asText();
        MvcResult removed = mvc.perform(delete("/api/v1/drafts/" + id + "/picks/" + firstPickId)
                        .header("Authorization", auth("alice")))
                .andExpect(status().isOk()).andReturn();
        JsonNode after = body(removed);
        assertThat(after.get("pickCount").asInt()).isEqualTo(2);
        assertThat(after.get("picks").get(0).get("pickNumber").asInt()).isEqualTo(1);
        assertThat(after.get("picks").get(1).get("pickNumber").asInt()).isEqualTo(2);
    }

    @Test
    void overseasLimitIsEnforcedByTheBackend() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();
        // overseas in the pool: Bat Three (NZ), AR Two (ENG), Bowl Three (SL), Bat Five (AUS), Bat Six (ENG)
        for (String n : List.of("Bat Three", "AR Two", "Bowl Three", "Bat Five")) {
            add("alice", id, pickByName(d, n).get("id").asText(), 201);
        }
        add("alice", id, pickByName(d, "Bat Six").get("id").asText(), 422);
    }

    @Test
    void ineligiblePlayerIsRejected() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();
        String thinMiPlayer = jdbc.queryForObject(
                "select ps.id from player_seasons ps join players p on p.id = ps.player_id where p.full_name = 'Thin Roster One'",
                String.class);
        add("alice", id, thinMiPlayer, 422);
        add("alice", id, java.util.UUID.randomUUID().toString(), 422);
    }

    // ------------------------------------------------------------ completion

    @Test
    void incompleteXiCannotBeCompleted() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();
        add("alice", id, pickByName(d, "Keeper One").get("id").asText(), 201);
        mvc.perform(post("/api/v1/drafts/" + id + "/complete").header("Authorization", auth("alice")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void fullLifecycleCompletesPersistsAndBecomesImmutable() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();
        for (String n : validXiNames()) {
            add("alice", id, pickByName(d, n).get("id").asText(), 201);
        }
        MvcResult r = mvc.perform(post("/api/v1/drafts/" + id + "/complete").header("Authorization", auth("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isComplete").value(true)).andReturn();
        JsonNode result = body(r);
        assertThat(result.get("teamRating").asInt()).isBetween(1, 100);
        assertThat(result.get("picks")).hasSize(11);
        assertThat(result.get("overseasPlayers").asInt()).isEqualTo(3);

        // immutable
        add("alice", id, pickByName(d, "Bat Five").get("id").asText(), 409);
        String pickId = result.get("picks").get(0).get("pickId").asText();
        mvc.perform(delete("/api/v1/drafts/" + id + "/picks/" + pickId).header("Authorization", auth("alice")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/drafts/" + id + "/complete").header("Authorization", auth("alice")))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/drafts/" + id).header("Authorization", auth("alice")))
                .andExpect(status().isConflict());

        // persisted + statistics + history + leaderboard
        mvc.perform(get("/api/v1/drafts/" + id + "/result").header("Authorization", auth("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.teamRating").value(result.get("teamRating").asInt()));
        mvc.perform(get("/api/v1/me/statistics").header("Authorization", auth("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedDrafts").value(1))
                .andExpect(jsonPath("$.leaderboardPosition").value(1))
                .andExpect(jsonPath("$.highestRating").value(result.get("teamRating").asInt()));
        mvc.perform(get("/api/v1/drafts").header("Authorization", auth("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        MvcResult lb = mvc.perform(get("/api/v1/leaderboard")).andExpect(status().isOk()).andReturn();
        JsonNode first = body(lb).get("content").get(0);
        assertThat(first.get("rank").asInt()).isEqualTo(1);
        assertThat(first.has("email")).isFalse();
        assertThat(first.has("firebaseUid")).isFalse();
    }

    // ------------------------------------------------------------ expert mode

    @Test
    void expertModeHidesRatingsUntilCompleted() throws Exception {
        JsonNode d = startDraft("alice", "EXPERT");
        String id = d.get("draftId").asText();
        assertThat(d.get("ratingsHidden").asBoolean()).isTrue();
        for (JsonNode p : d.get("players")) {
            assertThat(p.get("rating").isNull()).isTrue();
        }
        JsonNode afterPick = add("alice", id, pickByName(d, "Keeper One").get("id").asText(), 201);
        assertThat(afterPick.get("picks").get(0).get("player").get("rating").isNull()).isTrue();
        String inProgress = mvc.perform(get("/api/v1/drafts/" + id).header("Authorization", auth("alice")))
                .andReturn().getResponse().getContentAsString();
        assertThat(inProgress).doesNotContain("\"rating\":7").doesNotContain("\"rating\":8");

        for (String n : validXiNames().subList(1, 11)) {
            add("alice", id, pickByName(d, n).get("id").asText(), 201);
        }
        MvcResult done = mvc.perform(post("/api/v1/drafts/" + id + "/complete").header("Authorization", auth("alice")))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode p : body(done).get("picks")) {
            assertThat(p.get("player").get("rating").isInt()).isTrue(); // revealed after completion
        }
    }

    // ------------------------------------------------------------ isolation & sharing

    @Test
    void usersCannotAccessEachOthersDrafts() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();
        String psId = pickByName(d, "Keeper One").get("id").asText();

        mvc.perform(get("/api/v1/drafts/" + id).header("Authorization", auth("mallory"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/drafts/" + id + "/picks").header("Authorization", auth("mallory"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"playerSeasonId\":\"" + psId + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/drafts/" + id + "/complete").header("Authorization", auth("mallory")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/drafts/" + id).header("Authorization", auth("mallory"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/drafts").header("Authorization", auth("mallory")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/drafts/" + id).header("Authorization", auth("alice"))).andExpect(status().isOk());
    }

    @Test
    void sharingExposesOnlyPublicDataAndCanBeRevoked() throws Exception {
        JsonNode d = startDraft("alice", "CLASSIC");
        String id = d.get("draftId").asText();
        // cannot share an unfinished draft
        mvc.perform(post("/api/v1/drafts/" + id + "/share").header("Authorization", auth("alice")))
                .andExpect(status().isConflict());

        for (String n : validXiNames()) {
            add("alice", id, pickByName(d, n).get("id").asText(), 201);
        }
        mvc.perform(post("/api/v1/drafts/" + id + "/complete").header("Authorization", auth("alice"))).andExpect(status().isOk());

        MvcResult shared = mvc.perform(post("/api/v1/drafts/" + id + "/share").header("Authorization", auth("alice")))
                .andExpect(status().isOk()).andReturn();
        String token = body(shared).get("shareToken").asText();
        assertThat(token.length()).isGreaterThanOrEqualTo(24);

        MvcResult publicView = mvc.perform(get("/api/v1/shared/" + token)).andExpect(status().isOk()).andReturn();
        String raw = publicView.getResponse().getContentAsString();
        assertThat(raw).doesNotContain("alice").doesNotContain("firebase").doesNotContain("email");

        mvc.perform(delete("/api/v1/drafts/" + id + "/share").header("Authorization", auth("alice")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/shared/" + token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/shared/not-a-real-token-at-all-xyz")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ other modes

    @Test
    void challengeModeAppliesExtraRulesAndGeneratesChallengeCode() throws Exception {
        JsonNode d = startDraft("alice", "CHALLENGE");
        assertThat(d.get("challengeCode").asText()).startsWith("CH-");
        assertThat(d.get("rules").get("maxOverseas").asInt()).isEqualTo(3);
        assertThat(d.get("rules").get("minAllRounders").asInt()).isEqualTo(1);
    }

    @Test
    void dreamModeDrawsFromTheWholeDatabase() throws Exception {
        JsonNode d = startDraft("alice", "DREAM");
        assertThat(d.get("multiSeason").asBoolean()).isTrue();
        assertThat(d.get("team").isNull()).isTrue();
        assertThat(d.get("season").isNull()).isTrue();
        assertThat(d.get("players").size()).isEqualTo(23);
    }

    @Test
    void draftHistorySupportsPaginationAndDeletionOfInProgressDrafts() throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(startDraft("alice", "CLASSIC").get("draftId").asText());
        }
        mvc.perform(get("/api/v1/drafts?page=0&size=2").header("Authorization", auth("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(delete("/api/v1/drafts/" + ids.get(0)).header("Authorization", auth("alice")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/drafts/" + ids.get(0)).header("Authorization", auth("alice")))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------ importer

    @Test
    void importerRejectsInvalidRowsAndIsIdempotent() throws Exception {
        String bad = """
                player_name,country,team_code,season,role,rating
                Good Player,India,RR,2023,BATTER,70
                Bad Rating,India,RR,2023,BATTER,150
                Bad Team,India,XXX,2023,BATTER,70
                Bad Season,India,RR,1999,BATTER,70
                Bad Role,India,RR,2023,GOALIE,70
                """;
        ImportSummary strict = importer.importCsv(new StringReader(bad), "bad", true);
        assertThat(strict.aborted()).isTrue();
        assertThat(strict.invalid()).isEqualTo(4);
        assertThat(strict.imported()).isZero();

        ImportSummary lenient = importer.importCsv(new StringReader(bad), "bad", false);
        assertThat(lenient.imported()).isEqualTo(1);
        ImportSummary again = importer.importCsv(new StringReader(bad), "bad", false);
        assertThat(again.imported()).isZero();
        assertThat(again.skipped()).isEqualTo(1);
    }

    @Test
    void flywayMigrationsCreatedTheSchemaAndFranchises() {
        Integer teams = jdbc.queryForObject("select count(*) from teams", Integer.class);
        assertThat(teams).isEqualTo(10);
        Integer constraints = jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints where table_name = 'player_seasons' and constraint_type = 'UNIQUE'",
                Integer.class);
        assertThat(constraints).isGreaterThanOrEqualTo(1);
    }
}
