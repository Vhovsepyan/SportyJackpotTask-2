package com.sporty.jackpot.service;

import java.math.BigDecimal;

/** Outcome of evaluating a bet for a jackpot reward. {@code rewardAmount} is null when the bet lost. */
public record RewardResult(String betId, boolean won, BigDecimal rewardAmount) {

    public static RewardResult won(String betId, BigDecimal rewardAmount) {
        return new RewardResult(betId, true, rewardAmount);
    }

    public static RewardResult lost(String betId) {
        return new RewardResult(betId, false, null);
    }
}
