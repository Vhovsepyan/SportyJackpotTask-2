package com.sporty.jackpot.domain;

/**
 * How a bet's chance of winning the jackpot is calculated.
 * Adding a new calculation = new enum value + new {@code RewardStrategy} implementation.
 */
public enum RewardType {
    FIXED_CHANCE,
    VARIABLE_CHANCE
}
