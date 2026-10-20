# IPL Draft Club

An **unofficial fan project**: spin for an IPL franchise and season, draft an XI from that exact roster, get it scored by the server, save it, share it and climb a leaderboard. Not affiliated with the BCCI, the IPL or any franchise. No club logos are used (text badges only).

> ## ⚠️ Read this first — verification status
> This codebase was generated in a sandbox whose network policy **blocked Maven Central**, so:
>
> | Part | Status |
> |---|---|
> | Frontend production build (`vite build`) | ✅ built successfully |
> | Frontend tests (21, Vitest) | ✅ all passing |
> | Backend compile + unit/integration tests (JUnit) | ❌ **never compiled or run** — please run `mvn test` (see below) and fix anything it reports |
> | Docker Compose / live Postgres / live Firebase login | ❌ not run |
>
> The backend was reviewed line-by-line, but expect the odd compile error or test mismatch on first run.
> The bundled player data is a **development sample** (see [Data accuracy](#data-accuracy)).

## Features
- Firebase Auth: email/password, Google sign-in, email verification, password reset, persistent session, protected routes.
- Spring Boot validates the Firebase **ID token on every request**; the UID only ever comes from the verified token.
- Four modes: **Classic**, **Expert** (ratings hidden by the server until completion), **Random Challenge** (3 overseas max, all-rounder required, rating budget, challenge code, locked once started) and **Dream XI** (multi-season, max 5 per franchise).
- Roulette spin UI — but the **backend chooses** the franchise/season, only from combinations whose roster can actually produce a valid XI.
- Server-side rules (centralised in `game/RuleSets.java`), duplicate prevention, ownership checks, immutable completed drafts, deterministic scoring, history, dashboard, leaderboard, share links (revocable).

## Architecture
```
React (Vite, Tailwind, TanStack Query)  ──Bearer Firebase ID token──▶  Spring Boot 3 (Java 21)
        │                                                                 │  verifies token (Firebase Admin SDK)
        └── Firebase Auth (identity only)                                 └─▶ PostgreSQL (Flyway-managed schema)
```
Firebase handles identity only. **All application data lives in PostgreSQL.** Passwords are never stored by this app.

## Folder structure
```
ipl-draft-club/
├── backend/   pom.xml, Dockerfile, src/main/java/com/ipldraftclub/{config,security,controller,service,repository,entity,dto,mapper,exception,game,seed}
│              src/main/resources/{application.yml, db/migration/, seed/sample_player_seasons.csv}
├── frontend/  Vite app: src/{app,components,pages,contexts,services,firebase,hooks,utils,styles,test}
├── docker-compose.yml  .env.example  .gitignore  secrets/ (git-ignored)
```

## Prerequisites
Java 21, Maven 3.9+, Node 20+ (22 recommended), PostgreSQL 16 (or Docker), a Firebase project.

## Firebase setup
Two **different** configurations are involved:

1. **Browser (public web config)** → `frontend/.env`
   - Firebase console → Project settings → *Your apps* → add a **Web app** → copy `apiKey`, `authDomain`, `projectId`, `appId`, `messagingSenderId` into the `VITE_FIREBASE_*` variables.
2. **Server (Admin SDK)** → `backend` environment
   - Set `FIREBASE_PROJECT_ID` to the same project id. Token *verification* only needs the project id (it uses Google's public signing keys).
   - For privileged Admin APIs, supply credentials: either `FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/service-account.json` (Project settings → Service accounts → *Generate new private key*; keep it **outside git** — `secrets/` is ignored) or Application Default Credentials / a secret manager in production.
   - Never put the service-account key in the frontend or commit it.

**Enable sign-in methods:** Authentication → Sign-in method → enable **Email/Password** and **Google**.
**Authorized domains:** Authentication → Settings → *Authorized domains* → add every host that serves the frontend (`localhost` is there by default; add your production domain). Otherwise Google sign-in fails with `auth/unauthorized-domain`.
**Email verification / password reset** use Firebase's built-in emails (customise templates under Authentication → Templates). Set `REQUIRE_EMAIL_VERIFIED=true` on the backend to block unverified password accounts.

## Running locally (without Docker)
```bash
# 1. Database
createdb ipl_draft   # or run: docker run -d --name iplpg -e POSTGRES_DB=ipl_draft -e POSTGRES_USER=ipl_user -e POSTGRES_PASSWORD=change_me -p 5432:5432 postgres:16-alpine

# 2. Backend  (Flyway migrates automatically; sample data seeds in the dev profile)
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/ipl_draft DB_USERNAME=ipl_user DB_PASSWORD=change_me
export FIREBASE_PROJECT_ID=<your-project-id> CORS_ALLOWED_ORIGINS=http://localhost:5173
mvn spring-boot:run              # http://localhost:8080/api/v1/health

# 3. Frontend
cd ../frontend
cp .env.example .env             # fill in the Firebase web config
npm install && npm run dev       # http://localhost:5173
```

## Running with Docker Compose
```bash
cp .env.example .env             # set POSTGRES_PASSWORD, FIREBASE_*, VITE_FIREBASE_*
docker compose up --build -d     # start (db → backend → frontend)
docker compose logs -f backend   # view logs
docker compose stop              # stop (keeps data)       docker compose down   # remove containers, KEEPS the pgdata volume
docker compose up --build -d backend   # rebuild one service
# Reset dev data (DESTRUCTIVE - deletes the database volume), only when you really mean it:
docker compose down && docker volume rm "$(basename "$PWD")_pgdata"
```
Place a service-account file at `secrets/service-account.json` and set `FIREBASE_SERVICE_ACCOUNT_PATH=/run/secrets/firebase/service-account.json` if you need Admin privileges.

## Database & migrations
Flyway owns the schema (`backend/src/main/resources/db/migration`): `V1__schema.sql` (tables, FKs, unique/check constraints, indexes) and `V2__seed_franchises.sql` (the ten franchises only). Hibernate runs with `ddl-auto: validate` in every profile — it never creates tables. Timestamps are UTC (`TIMESTAMP WITH TIME ZONE`).

Key constraints: `player_seasons(player_id, team_id, season)` unique; `draft_picks(draft_id, pick_number)` and `(draft_id, player_season_id)` unique; rating 1–100; pick number 1–11. Draft mutations run in a transaction under a pessimistic row lock (plus an optimistic `@Version`).

## Data accuracy
`backend/src/main/resources/seed/sample_player_seasons.csv` is a **development sample, not an official or complete dataset**: IPL **2023 only**, CSK/MI/RCB, ~15 players each, compiled by hand and **unverified**; ratings are **illustrative** numbers, not statistics. Because only 2023 exists, the game can only ever offer IPL 2023 — no other season is invented or copied. Replace it with verified data before real use.

### Adding a new season safely
1. Prepare a CSV with columns `player_name,country,team_code,season,role,rating` (optional: `batting_style,bowling_style,default_role,overseas,active`). Roles: `WICKETKEEPER|BATTER|ALL_ROUNDER|BOWLER`. Only add rows you have **verified** for that exact franchise and season — a player who moved teams needs a separate row per franchise-season.
2. Import: `SEED_CSV_PATH=/path/to/ipl2024.csv` (runs at startup via `SeedDataRunner`) or call `RosterCsvImporter.importCsv(reader, source, strict)` from your own runner. **Strict mode** (used at startup) aborts the whole file if any row is invalid.
3. Every row is validated: known franchise code, season 2008–next year, valid role, rating 1–100, sane country, and `overseas` consistent with the country. A summary (imported / skipped / invalid + per-row reasons) is logged. Re-importing is idempotent.
4. A franchise-season becomes selectable **only** when its roster can form a valid XI (≥11 players, a keeper, a batter, 3 bowlers, enough domestic players for the overseas cap). `GET /teams` lists only such seasons, so an incomplete import can never produce an empty pool.

## API (`/api/v1`)
Public: `GET /health`, `/teams`, `/teams/{code}/seasons`, `/teams/{code}/seasons/{season}/players` (ratings omitted; 404 when no roster), `/leaderboard?page&size&sort=points|average|highest|completed`, `/game-modes`, `/shared/{token}`.
Authenticated (`Authorization: Bearer <Firebase ID token>`): `GET /me`, `PUT /me/profile`, `GET /me/statistics`, `POST /drafts/start {mode}` (optional `Idempotency-Key`), `GET /drafts?page&size&status`, `GET /drafts/{id}`, `POST /drafts/{id}/picks {playerSeasonId}`, `DELETE /drafts/{id}/picks/{pickId}`, `POST /drafts/{id}/complete`, `GET /drafts/{id}/result`, `DELETE /drafts/{id}` (in-progress only), `POST|DELETE /drafts/{id}/share`.
Errors: `{timestamp,status,error,message,path,details[]}`; 401 unauthenticated, 404 for drafts you don't own, 409 completed/immutable, 422 rule violation or no eligible pool.

### Scoring (deterministic, server-side)
Batting/bowling are role-weighted means of player ratings; all-round = mean of all-rounders × min(count,2)/2; wicketkeeping = best keeper; role balance = 100 − 8×|deviation from 1 WK/4 BAT/2 AR/4 BOWL|.
`teamRating = round(0.30·bat + 0.30·bowl + 0.10·allRound + 0.10·keeper + 0.20·balance)`. It is a transparent index, **not** a match prediction. Leaderboard points = rating × mode multiplier (Expert ×1.25, Challenge ×1.5).

## Tests
```bash
cd backend  && mvn test        # JUnit 5 unit tests + Spring Boot integration tests (H2 in PostgreSQL mode)
cd frontend && npm test        # Vitest + Testing Library (21 tests)
cd frontend && npm run build
```
Backend tests cover: rule validation (duplicates, overseas cap, roles, budget, franchise cap), scoring, ownership isolation, completed-draft immutability, Expert-mode rating hiding, share/revoke, importer validation & idempotency, and that a franchise-season without a draftable roster is never offered or selected. Auth in tests uses a **test-only** `TokenVerifier` (`src/test/.../TestSecurityConfig`) — it is not part of production security configuration. Testcontainers is on the classpath for running the same suite against real PostgreSQL; the bundled tests use H2 for speed and portability.

## Troubleshooting
- **Blank/"not configured" auth pages** → fill `frontend/.env` and restart `npm run dev`.
- **`auth/unauthorized-domain`** → add the domain in Firebase Authorized domains.
- **401 from the API with a valid login** → `FIREBASE_PROJECT_ID` on the backend must equal the web app's project id.
- **CORS errors** → set `CORS_ALLOWED_ORIGINS` to the exact frontend origin (no wildcard).
- **"No playable rosters yet"** → no roster data imported; check backend logs for the import summary.
- **Backend can't connect to DB in Docker** → wait for the `db` health check; check `POSTGRES_PASSWORD`.

## Production notes
Serve over HTTPS; use `SPRING_PROFILES_ACTIVE=prod` (sample seeding off); keep secrets in a secret manager; build the frontend with `npm run build` and serve `dist/` from a static host/CDN (the provided frontend Dockerfile is a **dev server**); set `CORS_ALLOWED_ORIGINS` to your real origin; the in-memory rate limiter is per-instance (use a gateway/Redis limiter when scaling out).

## Known limitations
- Backend not yet compiled/tested by the author (see top). Playwright E2E tests were not written.
- Only a small, unverified 2023 sample roster is included; ratings are illustrative.
- Random Challenge records completion time and a challenge code but has no per-challenge shared leaderboard; the "challenge identifier" is unique per start, not a reusable shared seed.
- Draft cloning is not implemented (start a new draft instead).
- Dream XI role feasibility under the per-franchise cap is only approximated when deciding availability.
