package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VariableChanceRewardTest {

    private final VariableChanceReward strategy = new VariableChanceReward();

    /** startChance 1% at initial pool 5000, reaching 100% at pool limit 10000. */
    private Jackpot jackpot(String currentPool) {
        return Jackpot.builder()
                .id("j2")
                .initialPool(new BigDecimal("5000"))
                .currentPool(new BigDecimal(currentPool))
                .rewardType(RewardType.VARIABLE_CHANCE)
                .rewardChance(new BigDecimal("0.01"))
                .rewardPoolLimit(new BigDecimal("10000"))
                .build();
    }

    @Test
    void typeIsVariableChance() {
        assertThat(strategy.type()).isEqualTo(RewardType.VARIABLE_CHANCE);
    }

    @Test
    void startChanceAtInitialPool() {
        assertThat(strategy.winChance(jackpot("5000"))).isEqualTo(0.01);
    }

    @Test
    void interpolatesAtMidpoint() {
        // progress 0.5 -> 0.01 + 0.99 * 0.5 = 0.505
        assertThat(strategy.winChance(jackpot("7500"))).isCloseTo(0.505, within(1e-9));
    }

    @Test
    void fullChanceAtPoolLimit() {
        assertThat(strategy.winChance(jackpot("10000"))).isEqualTo(1.0);
    }

    @Test
    void fullChanceAbovePoolLimit() {
        assertThat(strategy.winChance(jackpot("15000"))).isEqualTo(1.0);
    }

    @Test
    void startChanceWhenPoolBelowInitial() {
        assertThat(strategy.winChance(jackpot("4000"))).isEqualTo(0.01);
    }
}
