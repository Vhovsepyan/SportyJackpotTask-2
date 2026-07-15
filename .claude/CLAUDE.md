# Jackpot Service — Step-by-Step Implementation Plan for Claude Code (v2)

Build a Spring Boot backend that processes bets for jackpot pool contributions and evaluates them for jackpot rewards. This is an interview assignment: the graders explicitly evaluate **system design and resilience**, not just working code. v1 of this assignment was rejected for: weak Kafka partitioning, unhandled update conflicts, missing business validation before publishing (duplicate bets, jackpot existence), an inflexible jackpot model, and not persisting unsuccessful reward evaluations. Every one of those is now a HARD requirement below — do not treat them as optional polish.

## HARD REQUIREMENT: 100% EXECUTABLE
The final product must run out of the box on a clean machine with only a JDK installed:
- Default profile is `mock` — the app MUST start and be fully usable with `./gradlew bootRun` and **zero external dependencies** (no Kafka broker, no Docker, no external DB — H2 is in-memory).
- The Gradle wrapper (`gradlew`, `gradlew.bat`, wrapper jar) MUST be committed so no local Gradle install is needed.
- At the end of EVERY step, verify both `./gradlew test` (all green) and `./gradlew bootRun` (starts cleanly, no stack traces). Never commit a broken build.
- Before the final commit, do a clean verification: `./gradlew clean build` then `./gradlew bootRun`, and exercise every endpoint with curl including the failure cases (duplicate bet → 409, unknown jackpot → 404, repeat evaluation → identical recorded outcome).

## HARD REQUIREMENTS: RESILIENCE & DESIGN (the reasons v1 failed)
1. **Partition by jackpot**: Kafka messages are keyed by `jackpotId`, NOT `betId`, so all updates to one jackpot are serialized on one partition and lock conflicts become structurally rare. State this reasoning in the README.
2. **Handle update conflicts**: optimistic locking (`@Version`) plus an automatic retry (spring-retry `@Retryable` with backoff, 3 attempts) around contribution processing. A concurrent bet must never be silently lost. Mock and Kafka modes must share identical retry semantics (retry lives in the service, not the transport).
3. **Business validation before publishing**: `POST /api/bets` must synchronously reject, BEFORE returning 202 and publishing: unknown `jackpotId` → 404; duplicate `betId` (already contributed or already accepted) → 409. The consumer additionally validates independently (idempotent skip on duplicates, warn-and-skip on unknown jackpot, reject non-positive/null amounts) because in Kafka mode messages can arrive from other producers.
4. **Persist every evaluation outcome**: losing evaluations are recorded, not just wins. Re-evaluating any bet returns the recorded outcome — a losing bet can NEVER be re-rolled into a win. Concurrent duplicate evaluations must converge on one recorded outcome (catch the unique-constraint race and return the persisted result, never a 500).
5. **Flexible jackpot model**: strategy parameters are NOT flattened as nullable columns. Each jackpot stores a typed config object per strategy (sealed interface + record implementations, persisted as JSON via a JPA `AttributeConverter`). Config type must match the configured strategy enum — validated at write/seed time so misconfiguration fails at startup, not at bet time with an NPE.
6. **Poison-message safety (kafka profile)**: `ErrorHandlingDeserializer` wrapping the JSON deserializer, plus a `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer` to `jackpot-bets.DLT`. A malformed record must never block the partition.
7. **Profile safety**: the mock publisher is `@Profile("!kafka")` so the app starts under ANY profile; only the explicit `kafka` profile switches transports.

## WORKING RULES (follow strictly)
1. Work through the steps below **in order, one at a time**. Do not jump ahead or merge steps.
2. After completing each step: run `./gradlew test` — all tests must pass before moving on.
3. Then commit with the message given in the step and **push to origin main** (`git add -A && git commit -m "..." && git push`).
4. If the remote is not configured yet, ask me for the GitHub repo URL before the first push.
5. Every step that adds logic must also add its tests **in the same step** — never defer tests to the end.
6. Use BigDecimal for all money values (win chance may be double). Money rounding policy (scale 4, HALF_UP) is defined ONCE as a shared constant, not per strategy class.
7. API responses are typed records (never `Map.of`), so springdoc documents real schemas. Errors use Spring's RFC-9457 `ProblemDetail` via a handler extending `ResponseEntityExceptionHandler` (400 validation, 404, 409, 500) so framework exceptions keep their correct 4xx mappings.
8. Entities: no `@Setter` on write-once records (contributions, evaluations); use `@CreationTimestamp` for createdAt; mutate only what is meant to mutate (`Jackpot.currentPool`).
9. Tests reference seeded IDs via constants (never string literals), use Awaitility for async waits (no hand-rolled sleep loops), and each `@SpringBootTest` context gets its own H2 database name so cached contexts never share/wipe state.
10. No auth. Beyond the requirements above, no further speculative abstraction.
11. After the final step, print a summary of all endpoints, seeded jackpot IDs, and example curl commands.

## Stack
- **Java 25**, **Gradle** (Kotlin DSL, wrapper committed, Java toolchain set to 25)
- Spring Boot 3.5.x (latest patch — required for Java 25 support)
- Spring Web, Spring Data JPA, H2 (in-memory), Spring Kafka, spring-retry (+ spring-boot-starter-aop), Lombok (latest, Java 25 compatible), Bean Validation, Jackson (for config JSON)
- springdoc-openapi for Swagger UI
- JUnit 5, Mockito, AssertJ, Awaitility for tests
- Base package: **`com.sporty.jackpot`**
- If any dependency fails on Java 25, fix via version bump or documented flag rather than downgrading Java. Only if genuinely blocked, tell me before changing anything.

---

## STEP 1 — Project scaffold
- Generate a Gradle (Kotlin DSL) Spring Boot 3.5.x project, base package `com.sporty.jackpot`, dependencies: web, data-jpa, h2, kafka, spring-retry + aop, lombok, validation, springdoc-openapi, spring-boot-starter-test, awaitility.
- `build.gradle.kts` with Java 25 toolchain; commit the Gradle wrapper files.
- `application.yml`: H2 in-memory (named DB for the console), H2 console enabled, default active profile `mock`, `open-in-view: false`. Do not restate framework defaults (driver-class-name, default paths).
- `.gitignore` for Gradle/IDE files (but NOT the wrapper).
- Package structure: `api` (controllers, request/response records, ProblemDetail handler), `domain` (entities, enums, strategy config records), `repository`, `service`, `strategy` (contribution/, reward/, factories), `kafka`, `config`.
- Test: default context-load test passes.
- Commit: `chore: scaffold Spring Boot Gradle project with Java 25 toolchain`. Push.

## STEP 2 — Domain model with typed strategy configs
- `ContributionType` { FIXED_PERCENTAGE, VARIABLE_PERCENTAGE }, `RewardType` { FIXED_CHANCE, VARIABLE_CHANCE }.
- Sealed interface `ContributionConfig` permitting records `FixedPercentageConfig(BigDecimal percentage)` and `VariablePercentageConfig(BigDecimal startPercentage, BigDecimal decayRate, BigDecimal floorPercentage)`; sealed interface `RewardConfig` permitting `FixedChanceConfig(BigDecimal chance)` and `VariableChanceConfig(BigDecimal startChance, BigDecimal poolLimit)`. Persist each via a Jackson-based JPA `AttributeConverter` (JSON column, `@type` discriminator).
- `Jackpot`: id (String), initialPool, currentPool, contributionType + contributionConfig, rewardType + rewardConfig, `@Version`. A `@PrePersist/@PreUpdate` check (or dedicated validator invoked by the seeder/service) asserts config class matches the enum — mismatches throw at write time with a clear message.
- `JackpotContribution`: id (UUID), betId (unique), userId, jackpotId, stakeAmount, contributionAmount, currentJackpotAmount, `@CreationTimestamp` createdAt. No `@Setter`.
- `JackpotEvaluation`: id (UUID), betId (unique), userId, jackpotId, won (boolean), rewardAmount (nullable — null on loss), `@CreationTimestamp` createdAt. No `@Setter`. **Every evaluation is persisted, wins and losses.**
- `Bet` record: betId, userId, jackpotId, amount.
- Repositories for the three entities with `findByBetId`/`existsByBetId` where needed.
- Seed via `CommandLineRunner` in `config`: `jackpot-fixed` (FIXED/FIXED) and `jackpot-variable` (VARIABLE/VARIABLE), IDs exposed as public constants and logged. Seeder runs the config-type validation.
- Tests: `@DataJpaTest` per repository (round-trip incl. JSON config round-trip, findByBetId, unique betId constraint violation); config/enum mismatch rejection test.
- Commit: `feat: add domain model with typed strategy configs, repositories and seed data`. Push.

## STEP 3 — Contribution strategies (with unit tests)
- `ContributionStrategy { ContributionType type(); BigDecimal calculate(BigDecimal betAmount, Jackpot jackpot); }` — implementations cast the typed config; shared `MONEY_SCALE` constant lives on the interface.
- `FixedPercentageContribution`: contribution = percentage × betAmount.
- `VariablePercentageContribution`: `pct = max(floorPct, startPct − decayRate × max(0, currentPool − initialPool))`.
- `ContributionStrategyFactory`: plain loop building an `EnumMap`, duplicate registration throws; unknown type throws.
- Unit tests: fixed math + rounding; variable decay, floor, at/below initial pool; factory resolves every enum value, throws on unregistered type and duplicates.
- Commit: `feat: add contribution strategies with unit tests`. Push.

## STEP 4 — Reward strategies (with unit tests)
- `RewardStrategy { RewardType type(); double winChance(Jackpot jackpot); }` (0.0–1.0).
- `FixedChanceReward`: constant chance. `VariableChanceReward`: linear from startChance at initialPool to 1.0 at poolLimit; 1.0 at/above poolLimit; guard division when poolLimit ≤ initialPool.
- `RewardStrategyFactory` mirroring Step 3.
- Unit tests: fixed; interpolation start/midpoint/limit/above-limit/below-initial; factory resolution + failure modes.
- Commit: `feat: add reward strategies with unit tests`. Push.

## STEP 5 — Contribution flow (service + publisher/consumer) with idempotency and retry
- `BetPublisher` interface:
  - `MockBetPublisher` (`@Profile("!kafka")`): logs and invokes `ContributionService` directly.
  - `KafkaBetPublisher` (`@Profile("kafka")`): JSON publish to `jackpot-bets`, **message key = jackpotId** (partition-per-jackpot serialization — document why in the class javadoc and README).
- `BetConsumer` (`@Profile("kafka")`): `@KafkaListener(topics = "jackpot-bets")` → `ContributionService`.
- `ContributionService.processBet(Bet)` (single authority for business rules — both transports call it):
  - Validate independently of the REST layer: null/non-positive amount → warn and skip (consumer path) — the REST edge has already rejected these for HTTP callers.
  - Duplicate betId (contribution exists) → log and skip (idempotent redelivery).
  - Unknown jackpot → warn and skip.
  - Compute contribution via factory, add to `currentPool`, persist `JackpotContribution` snapshot — `@Transactional` + `@Retryable(OptimisticLockingFailureException, 3 attempts, small backoff)`; `@EnableRetry` on a config class. A concurrent conflict retries transparently in BOTH modes.
- Tests (`ContributionServiceTest`, Mockito): pool increment + snapshot values; duplicate betId skipped; unknown jackpot skipped; non-positive amount skipped; retry annotation present/behavioral test via a small `@SpringBootTest` slice proving a simulated version conflict is retried. `MockBetPublisherTest`: publish triggers processing.
- Commit: `feat: implement idempotent bet contribution flow with optimistic-lock retry`. Push.

## STEP 6 — API endpoint: publish bet (with business validation at the edge)
- `POST /api/bets` accepting `{betId, userId, jackpotId, amount}` (Bean Validation: non-blank IDs, amount not null and > 0). Controller flow: jackpot must exist → else 404; betId must be new → else 409; then publish via `BetPublisher`, return **202** with typed record `BetAcceptedResponse(betId, status)`.
- Error handling: `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`, `ProblemDetail` bodies; handlers for `ResourceNotFoundException` → 404, `DuplicateBetException` → 409, `OptimisticLockingFailureException`/`DataIntegrityViolationException` → 409, generic → 500 (logged).
- Tests: `@WebMvcTest` — valid → 202 + published; validation failures → 400 with field details; unknown jackpot → 404; duplicate betId → 409; malformed JSON → 400.
- Commit: `feat: add bet endpoint with duplicate and jackpot-existence validation`. Push.

## STEP 7 — Reward evaluation: every outcome persisted, race-safe
- `RewardService.evaluate(betId)`:
  - Existing `JackpotEvaluation` → return its recorded outcome (win OR loss — losses are terminal, never re-rolled).
  - No contribution for betId → 404.
  - Roll via injected `RandomProvider`; persist `JackpotEvaluation` for BOTH outcomes. On win: rewardAmount = current pool, reset pool to initialPool (same transaction).
  - Concurrent duplicate evaluation: unique betId constraint breaks the tie — catch the violation outside the failed transaction, re-read, and return the recorded outcome (never 500, never two different answers).
- `POST /api/bets/{betId}/evaluate` → typed record `{betId, won, rewardAmount}`.
- Tests: stubbed RandomProvider — win persists evaluation + resets pool; loss persists evaluation and changes nothing else; repeat after LOSS returns recorded loss without re-rolling (explicit anti-re-roll test); repeat after win returns same reward; unique-violation race path returns recorded outcome; unknown bet → 404. `@WebMvcTest`: win/loss/404.
- Commit: `feat: implement race-safe reward evaluation persisting all outcomes`. Push.

## STEP 8 — Real Kafka mode: partitioning, poison-pill safety, DLT + integration tests
- `docker-compose.yml`: single-node Kafka (KRaft, no Zookeeper). Optional tooling only — mock profile stays default and self-contained.
- `application-kafka.yml`: `ErrorHandlingDeserializer` wrapping `JsonDeserializer` (key: String), consumer group `jackpot-service`; container factory config with `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` → `jackpot-bets.DLT`.
- Integration tests:
  - Mock profile `@SpringBootTest` (own H2 name): POST bet → contribution + pool; duplicate POST → 409; evaluate → recorded outcome stable across repeat calls.
  - `@EmbeddedKafka` kafka profile (own H2 name): published bet keyed by jackpotId is consumed and contributed (Awaitility); malformed record goes to DLT and does NOT block subsequent messages.
  - Concurrency test (mock profile): N parallel bets on one jackpot → all N contributions recorded, pool = initial + Σ contributions (proves retry).
- Commit: `feat: add Kafka profile with jackpot-keyed partitioning, DLT and integration tests`. Push.

## STEP 9 — README, final executability check, polish
- `README.md` with an explicit **Design decisions** section (this is what reviewers read first): Strategy pattern + how to add a type; typed JSON strategy configs and why (extensible model, fail-fast validation); partitioning by jackpotId and why; optimistic locking + retry; edge validation (duplicate/unknown) with status codes; evaluation idempotency including persisted losses (anti-re-roll); DLT/poison-pill handling; profiles.
- Prerequisites (Java 25 only for mock; Docker only for kafka), run/test commands, curl examples for success AND failure cases, Swagger + H2 console URLs.
- Final check, in order: `./gradlew clean build`; `./gradlew bootRun`; curl: valid bet → 202, duplicate → 409, unknown jackpot → 404, evaluate → 200, repeat evaluate → identical outcome, unknown bet evaluate → 404; wrapper files committed.
- Commit: `docs: add README with design decisions and run instructions`. Push.
- Print final summary: endpoints, seeded jackpot IDs, curl commands, test count.
