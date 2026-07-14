package com.sporty.jackpot.domain;

/**
 * How a bet's contribution to the jackpot pool is calculated.
 * Adding a new calculation = new enum value + new {@code ContributionStrategy} implementation.
 */
public enum ContributionType {
    FIXED_PERCENTAGE,
    VARIABLE_PERCENTAGE
}
