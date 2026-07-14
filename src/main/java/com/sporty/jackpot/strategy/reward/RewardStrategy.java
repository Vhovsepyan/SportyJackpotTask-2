package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;

/** Calculates a bet's chance of winning the jackpot. */
public interface RewardStrategy {

    /** The configuration type this strategy implements. */
    RewardType type();

    /** Returns the win chance in the range 0.0–1.0, based on the jackpot's configuration. */
    double winChance(Jackpot jackpot);
}
