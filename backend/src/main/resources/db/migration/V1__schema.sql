-- IPL Draft Club schema (portable across PostgreSQL and H2 in PostgreSQL mode).
-- All timestamps are stored in UTC.

CREATE TABLE teams (
    id      UUID         NOT NULL PRIMARY KEY,
    code    VARCHAR(8)   NOT NULL,
    name    VARCHAR(100) NOT NULL,
    active  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_teams_code UNIQUE (code),
    CONSTRAINT uq_teams_name UNIQUE (name)
);

CREATE TABLE players (
    id             UUID         NOT NULL PRIMARY KEY,
    full_name      VARCHAR(120) NOT NULL,
    country        VARCHAR(60)  NOT NULL,
    batting_style  VARCHAR(40),
    bowling_style  VARCHAR(60),
    default_role   VARCHAR(20),
    CONSTRAINT uq_players_name_country UNIQUE (full_name, country),
    CONSTRAINT ck_players_default_role CHECK (default_role IS NULL OR default_role IN ('WICKETKEEPER','BATTER','ALL_ROUNDER','BOWLER'))
);

-- Season-specific roster: the ONLY source of truth for "who played for which franchise in which season".
CREATE TABLE player_seasons (
    id         UUID        NOT NULL PRIMARY KEY,
    player_id  UUID        NOT NULL,
    team_id    UUID        NOT NULL,
    season     INT         NOT NULL,
    role       VARCHAR(20) NOT NULL,
    rating     INT         NOT NULL,
    overseas   BOOLEAN     NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_ps_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_ps_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT uq_ps_player_team_season UNIQUE (player_id, team_id, season),
    CONSTRAINT ck_ps_rating CHECK (rating BETWEEN 1 AND 100),
    CONSTRAINT ck_ps_season CHECK (season BETWEEN 2008 AND 2100),
    CONSTRAINT ck_ps_role CHECK (role IN ('WICKETKEEPER','BATTER','ALL_ROUNDER','BOWLER'))
);
CREATE INDEX idx_ps_team_season ON player_seasons (team_id, season, active);
CREATE INDEX idx_ps_player ON player_seasons (player_id);

CREATE TABLE user_profiles (
    id            UUID         NOT NULL PRIMARY KEY,
    firebase_uid  VARCHAR(128) NOT NULL,
    display_name  VARCHAR(40)  NOT NULL,
    email         VARCHAR(254),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_user_profiles_uid UNIQUE (firebase_uid)
);

CREATE TABLE drafts (
    id                UUID         NOT NULL PRIMARY KEY,
    firebase_uid      VARCHAR(128) NOT NULL,
    mode              VARCHAR(20)  NOT NULL,
    team_id           UUID,
    season            INT,
    status            VARCHAR(20)  NOT NULL,
    challenge_code    VARCHAR(20),
    idempotency_key   VARCHAR(64),
    started_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at      TIMESTAMP WITH TIME ZONE,
    completion_seconds BIGINT,
    team_rating       INT,
    total_score       INT,
    points            INT,
    batting_score     INT,
    bowling_score     INT,
    all_round_score   INT,
    wicketkeeping_score INT,
    role_balance_score INT,
    overseas_count    INT,
    shared            BOOLEAN      NOT NULL DEFAULT FALSE,
    share_token       VARCHAR(64),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_drafts_user FOREIGN KEY (firebase_uid) REFERENCES user_profiles (firebase_uid),
    CONSTRAINT fk_drafts_team FOREIGN KEY (team_id) REFERENCES teams (id),
    CONSTRAINT ck_drafts_mode CHECK (mode IN ('CLASSIC','EXPERT','CHALLENGE','DREAM')),
    CONSTRAINT ck_drafts_status CHECK (status IN ('IN_PROGRESS','COMPLETED')),
    CONSTRAINT ck_drafts_rating CHECK (team_rating IS NULL OR team_rating BETWEEN 0 AND 100),
    CONSTRAINT ck_drafts_season CHECK (season IS NULL OR season BETWEEN 2008 AND 2100),
    CONSTRAINT uq_drafts_share_token UNIQUE (share_token),
    CONSTRAINT uq_drafts_idem UNIQUE (firebase_uid, idempotency_key)
);
CREATE INDEX idx_drafts_user_created ON drafts (firebase_uid, created_at);
CREATE INDEX idx_drafts_user_status ON drafts (firebase_uid, status);

CREATE TABLE draft_picks (
    id               UUID NOT NULL PRIMARY KEY,
    draft_id         UUID NOT NULL,
    player_season_id UUID NOT NULL,
    pick_number      INT  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_picks_draft FOREIGN KEY (draft_id) REFERENCES drafts (id) ON DELETE CASCADE,
    CONSTRAINT fk_picks_ps FOREIGN KEY (player_season_id) REFERENCES player_seasons (id),
    CONSTRAINT uq_picks_draft_number UNIQUE (draft_id, pick_number),
    CONSTRAINT uq_picks_draft_ps UNIQUE (draft_id, player_season_id),
    CONSTRAINT ck_picks_number CHECK (pick_number BETWEEN 1 AND 11)
);
CREATE INDEX idx_picks_draft ON draft_picks (draft_id);

CREATE TABLE user_statistics (
    id               UUID         NOT NULL PRIMARY KEY,
    firebase_uid     VARCHAR(128) NOT NULL,
    total_drafts     INT          NOT NULL DEFAULT 0,
    completed_drafts INT          NOT NULL DEFAULT 0,
    rating_sum       BIGINT       NOT NULL DEFAULT 0,
    average_rating   DOUBLE PRECISION NOT NULL DEFAULT 0,
    highest_rating   INT          NOT NULL DEFAULT 0,
    points           BIGINT       NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_stats_user FOREIGN KEY (firebase_uid) REFERENCES user_profiles (firebase_uid),
    CONSTRAINT uq_stats_uid UNIQUE (firebase_uid),
    CONSTRAINT ck_stats_nonneg CHECK (total_drafts >= 0 AND completed_drafts >= 0 AND points >= 0)
);
CREATE INDEX idx_stats_points ON user_statistics (points);
