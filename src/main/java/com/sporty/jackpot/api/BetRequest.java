package com.sporty.jackpot.api;

import com.sporty.jackpot.domain.Bet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Incoming bet payload for {@code POST /api/bets}. */
public record BetRequest(
        @NotBlank String betId,
        @NotBlank String userId,
        @NotBlank String jackpotId,
        @NotNull @Positive BigDecimal amount) {

    public Bet toBet() {
        return new Bet(betId, userId, jackpotId, amount);
    }
}
