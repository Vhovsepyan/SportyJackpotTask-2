package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;

import java.math.BigDecimal;

/** Calculates how much of a bet's stake goes into a jackpot pool. */
public interface ContributionStrategy {

    /** The configuration type this strategy implements. */
    ContributionType type();

    /** Returns the contribution amount for the given stake, based on the jackpot's configuration. */
    BigDecimal calculate(BigDecimal betAmount, Jackpot jackpot);
}
