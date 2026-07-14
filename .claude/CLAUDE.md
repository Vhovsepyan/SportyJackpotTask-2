# Jackpot Service — Step-by-Step Implementation Plan for Claude Code

Build a Spring Boot backend that processes bets for jackpot pool contributions and evaluates them for jackpot rewards. This is an interview assignment: prioritize clean OO design, extensibility, tests, and a fully runnable result.

## HARD REQUIREMENT: 100% EXECUTABLE
The final product must run out of the box on a clean machine with only a JDK installed:
- Default profile is `mock` — the app MUST start and be fully usable with `./gradlew bootRun` and **zero external dependencies** (no Kafka broker, no Docker, no external DB — H2 is in-memory).
- The Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper jar) MUST be committed so no local Gradle install is needed.
- At the end of EVERY step, verify both `./gradlew test` (all green) and `./gradlew bootRun` (starts cleanly, no stack traces). Never commit a broken build.
- Before the final commit, do a clean verification: `./gradlew clean build` then `./gradlew bootRun`, and exercise both endpoints with curl to confirm the full flow works end to end.

## WORKING RULES (follow strictly)
1. Work through the steps below **in order, one at a time**. Do not jump ahead or merge steps.
2. After completing each step: run `./gradlew test` — all tests must pass before moving on.
3. Then commit with the message given in the step and **push to origin main** (`git add -A && git commit -m "..." && git push`).
4. If the remote is not configured yet, ask me for the GitHub repo URL before the first push.
5. Every step that adds logic must also add its tests **in the same step** — never defer tests to the end.
6. Use BigDecimal for all money values. No auth, no over-engineering beyond what's specified.
7. After the final step, print a summary of all endpoints, seeded jackpot IDs, and example curl commands.

## Stack
- **Java 25**, **Gradle** (Kotlin DSL, wrapper committed, Java toolchain set to 25)
- Spring Boot 3.5.x (latest patch — required for Java 25 support)
- Spring Web, Spring Data JPA, H2 (in-memory), Spring Kafka, Lombok (latest version — must be Java 25 compatible), Bean Validation
- springdoc-openapi for Swagger UI
- JUnit 5, Mockito, AssertJ for tests
- Base package: **`com.sporty.jackpot`**
- If any dependency (e.g. Lombok or Mockito byte-buddy) fails on Java 25, fix it via version bump or documented flag rather than downgrading Java. Only if genuinely blocked, tell me before changing anything.

---

## STEP 1 — Project scaffold
- Generate a Gradle (Kotlin DSL) Spring Boot 3.5.x project, base package `com.sporty.jackpot`, with dependencies: web, data-jpa, h2, kafka, lombok, validation, springdoc-openapi, spring-boot-starter-test.
- `build.gradle.kts` with `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`; commit the Gradle wrapper files.
- `application.yml`: H2 in-memory config, H2 console enabled, default active profile `mock`.
- `.gitignore` for Gradle/IDE files (`build/`, `.gradle/`, `.idea/` — but NOT the wrapper).
- Empty package structure:
```
com.sporty.jackpot
├── api           (controllers, DTOs, exception handler)
├── domain        (entities, enums)
├── repository
├── service
├── strategy      (contribution/, reward/, factory)
├── kafka
└── config
```
- Test: the default context-load test passes.
- Verify: `./gradlew test` green, `./gradlew bootRun` starts.
- Commit: `chore: scaffold Spring Boot Gradle project with Java 25 toolchain`
- Push.

## STEP 2 — Domain model and repositories
- Entities (JPA, H2):
  - `Jackpot`: id (String), initialPool, currentPool, contributionType (enum) + contribution params, rewardType (enum) + reward params (poolLimit etc.), `@Version` for optimistic locking.
  - `JackpotContribution`: id, betId, userId, jackpotId, stakeAmount, contributionAmount, currentJackpotAmount, createdAt.
  - `JackpotReward`: id, betId, userId, jackpotId, jackpotRewardAmount, createdAt.
- `Bet` as a record: betId, userId, jackpotId, amount (contributions carry the persisted trace of bets).
- Spring Data repositories for the three entities, including `findByBetId` on contribution and reward repos.
- Seed data via `CommandLineRunner` in `config`: two jackpots — one FIXED/FIXED, one VARIABLE/VARIABLE — with clearly logged IDs (e.g. `jackpot-fixed`, `jackpot-variable`).
- Tests: `@DataJpaTest` for each repository (save + find round-trip, findByBetId).
- Verify, commit: `feat: add domain model, repositories and seed data`, push.

## STEP 3 — Contribution strategies (with unit tests)
- Interface `ContributionStrategy { BigDecimal calculate(BigDecimal betAmount, Jackpot jackpot); }`
- `FixedPercentageContribution`: contribution = pct × betAmount.
- `VariablePercentageContribution`: starts high, decreases at a fixed rate as pool grows above initial, with a floor:
  `pct = max(floorPct, startPct − decayRate × (currentPool − initialPool))`.
- `ContributionStrategyFactory` keyed on the enum; adding a new type = new class + enum value.
- Unit tests: fixed percentage math; variable decay over increasing pool; floor is respected; factory resolves every enum value and throws on unknown.
- Verify, commit: `feat: add contribution strategies with unit tests`, push.

## STEP 4 — Reward strategies (with unit tests)
- Interface `RewardStrategy { double winChance(Jackpot jackpot); }` (return 0.0–1.0).
- `FixedChanceReward`: constant percentage.
- `VariableChanceReward`: starts low, increases linearly with pool between initialPool and poolLimit; returns 1.0 when pool ≥ poolLimit.
- `RewardStrategyFactory` mirroring the contribution factory.
- Unit tests: fixed chance; interpolation at start / midpoint; chance = 100% at and above poolLimit; factory resolution.
- Verify, commit: `feat: add reward strategies with unit tests`, push.

## STEP 5 — Contribution flow (service + Kafka consumer + mock publisher)
- `BetPublisher` interface with two implementations selected by profile:
  - `MockBetPublisher` (`@Profile("mock")`): logs the payload and invokes the processing service directly (same code path as the consumer).
  - `KafkaBetPublisher` (`@Profile("kafka")`): `KafkaTemplate` JSON publish to topic `jackpot-bets`.
- `BetConsumer` (`@Profile("kafka")`): `@KafkaListener(topics = "jackpot-bets")` → delegates to `ContributionService`.
- `ContributionService.processBet(Bet)`:
  - Find jackpot by jackpotId; if absent, log a warning and skip.
  - Compute contribution via strategy factory, add to `currentPool`, save jackpot (transactional; optimistic locking handles concurrency), persist `JackpotContribution` snapshot.
- Tests:
  - `ContributionServiceTest` (Mockito): pool incremented correctly, contribution record persisted with correct snapshot values, unknown jackpot is skipped without error.
  - `MockBetPublisherTest`: publishing triggers processing.
- Verify, commit: `feat: implement bet contribution flow with Kafka consumer and mock publisher`, push.

## STEP 6 — API endpoint: publish bet
- `POST /api/bets` accepting `{betId, userId, jackpotId, amount}` with Bean Validation (non-null, amount > 0). Publishes via `BetPublisher`, returns **202 Accepted**.
- Global `@RestControllerAdvice` exception handler → clean JSON error responses (400 validation, 404, 409, 500).
- Tests: `@WebMvcTest` — valid bet returns 202 and calls publisher; invalid payload returns 400 with error details.
- Verify, commit: `feat: add bet publishing endpoint with validation and error handling`, push.

## STEP 7 — Reward evaluation flow + endpoint
- `RewardService.evaluate(betId)`:
  - If a reward already exists for betId → return it (idempotent).
  - Find the bet's contribution; if none → 404 (bet never contributed).
  - Get win chance from reward strategy, roll via an injected `RandomProvider` (injectable so tests are deterministic).
  - On win: persist `JackpotReward` with rewardAmount = current pool, reset pool to initialPool (transactional).
  - On loss: return a no-win result.
- `POST /api/bets/{betId}/evaluate` → response `{betId, won, rewardAmount}`.
- Tests:
  - `RewardServiceTest` with a stubbed RandomProvider: win path persists reward and resets pool; loss path changes nothing; idempotency returns the same reward on repeat calls; unknown bet → exception.
  - `@WebMvcTest`: win, loss, and 404 responses.
- Verify, commit: `feat: implement jackpot reward evaluation with idempotency`, push.

## STEP 8 — Real Kafka mode + integration test
- `docker-compose.yml` with single-node Kafka (KRaft, no Zookeeper). This is OPTIONAL tooling for the `kafka` profile only — the mock profile must remain the default and fully self-contained.
- Kafka JSON serializer/deserializer config for the `kafka` profile, consumer group `jackpot-service`.
- Integration test: `@SpringBootTest` on the **mock** profile — end-to-end: POST a bet → contribution recorded and pool increased → evaluate endpoint returns a valid win/lose response. (Optionally an `@EmbeddedKafka` test for the kafka profile if quick; do not block on it.)
- Verify, commit: `feat: add real Kafka profile with docker-compose and end-to-end integration test`, push.

## STEP 9 — README, final executability check, polish
- `README.md`:
  - Overview + design notes: why Strategy pattern, how to add a new contribution/reward type, concurrency handling, idempotent evaluation.
  - Prerequisites: **Java 25 only** for mock mode (Gradle wrapper included); Docker only for the optional kafka mode.
  - Run mock mode: `./gradlew bootRun`.
  - Run kafka mode: `docker compose up -d` then `./gradlew bootRun --args='--spring.profiles.active=kafka'`.
  - Run tests: `./gradlew test`.
  - curl examples for both endpoints using the seeded jackpot IDs; Swagger UI and H2 console URLs.
- Final executability check (do all of these, in order):
  1. `./gradlew clean build` — green.
  2. `./gradlew bootRun` — starts with no errors on the default mock profile.
  3. curl `POST /api/bets` with a seeded jackpot ID → 202, contribution logged.
  4. curl `POST /api/bets/{betId}/evaluate` → valid JSON win/lose response.
  5. Confirm the Gradle wrapper files are committed.
- Commit: `docs: add README with run instructions and design notes`, push.
- Print final summary: endpoints, seeded jackpot IDs, curl commands, test count.
