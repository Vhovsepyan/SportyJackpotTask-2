package com.sporty.jackpot.domain;

import java.math.BigDecimal;

/**
 * A bet as it travels through the system (API -> publisher -> consumer -> service).
 * Bets are not persisted themselves; {@link JackpotContribution} carries their persisted trace.
 */
public record Bet(String betId, String userId, String jackpotId, BigDecimal amount) {
}
