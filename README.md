# Jackpot Service

A Spring Boot backend that processes bets for jackpot pool contributions and evaluates them for jackpot rewards.

Every bet placed on a jackpot contributes a slice of its stake to that jackpot's pool. A bet can then be
evaluated for a reward: a chance-based roll that, on a win, pays out the entire current pool and resets it
to the jackpot's initial value.

## Design notes

### Strategy pattern for contribution & reward calculations
Each jackpot is configured with a `ContributionType` and a `RewardType` (stored on the `Jackpot` entity).
Two factories (`ContributionStrategyFactory`, `RewardStrategyFactory`) resolve the matching strategy bean at
runtime from all Spring-registered implementations:

- **Contribution** — `ContributionStrategy.calculate(betAmount, jackpot)`
  - `FIXED_PERCENTAGE`: contribution = pct × betAmount.
  - `VARIABLE_PERCENTAGE`: starts high and decays linearly as the pool grows above its initial value, with a
    floor: `pct = max(floorPct, startPct − decayRate × (currentPool − initialPool))`.
- **Reward** — `RewardStrategy.winChance(jackpot)` (0.0–1.0)
  - `FIXED_CHANCE`: constant win chance.
  - `VARIABLE_CHANCE`: starts low and grows linearly with the pool, reaching 100% when the pool hits the
    configured `rewardPoolLimit`.

**Adding a new type** = add an enum value + one new `@Component` class implementing the interface and
returning that enum from `type()`. The factory picks it up automatically; no existing code changes.

### Concurrency
The `Jackpot` entity carries a JPA `@Version` field. Concurrent pool updates (two bets contributing, or a
contribution racing a win/reset) fail fast with an optimistic-locking conflict instead of silently losing
updates; the API maps this to HTTP 409. In Kafka mode, retryable consumer errors simply reprocess the record.

### Idempotent reward evaluation
`RewardService.evaluate(betId)` first checks whether a reward already exists for the bet and returns it
unchanged — re-evaluating a winning bet never rolls again or pays twice. The win roll itself uses an
injectable `RandomProvider`, so tests are fully deterministic.

### Messaging abstraction
`POST /api/bets` hands the bet to a `BetPublisher`. The default `mock` profile wires `MockBetPublisher`,
which logs the payload and invokes `ContributionService` directly — the exact code path the Kafka consumer
uses. The `kafka` profile swaps in `KafkaBetPublisher` + `BetConsumer` over the `jackpot-bets` topic
(JSON-serialized, consumer group `jackpot-service`).

## Prerequisites

- **Mock mode (default): Java 25 only.** The Gradle wrapper is committed — no Gradle install, no Docker,
  no broker; the DB is in-memory H2.
- **Kafka mode (optional):** Docker, for the single-node KRaft Kafka in `docker-compose.yml`.

## Running

Mock mode (default):

```bash
./gradlew bootRun
```

Kafka mode:

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=kafka'
```

Tests:

```bash
./gradlew test
```

The app starts on **http://localhost:8080**. Two jackpots are seeded at startup:

| Jackpot ID         | Contribution                                              | Reward                                                 |
|--------------------|-----------------------------------------------------------|--------------------------------------------------------|
| `jackpot-fixed`    | fixed 5% of stake                                          | fixed 10% win chance                                   |
| `jackpot-variable` | starts at 10%, decays as pool grows, floor 2%              | starts at 1%, reaches 100% at pool limit 10,000        |

## API

### Publish a bet

```bash
curl -i -X POST http://localhost:8080/api/bets \
  -H "Content-Type: application/json" \
  -d '{"betId":"bet-1","userId":"user-1","jackpotId":"jackpot-fixed","amount":100}'
```

Returns `202 Accepted`; the bet is published and its jackpot contribution is processed asynchronously
(synchronously in mock mode).

### Evaluate a bet for a jackpot reward

```bash
curl -i -X POST http://localhost:8080/api/bets/bet-1/evaluate
```

Returns `200 OK` with `{"betId":"bet-1","won":true|false,"rewardAmount":<pool>|null}`.
Evaluating a bet that never contributed returns `404`.

### Tooling

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:jackpotdb`, user `sa`, empty password)
