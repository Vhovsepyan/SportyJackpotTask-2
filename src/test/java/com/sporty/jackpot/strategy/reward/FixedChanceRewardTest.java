package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FixedChanceRewardTest {

    private final FixedChanceReward strategy = new FixedChanceReward();

    private Jackpot jackpot(String chance, String currentPool) {
        return Jackpot.builder()
                .id("j1")
                .initialPool(new BigDecimal("1000"))
                .currentPool(new BigDecimal(currentPool))
                .rewardType(RewardType.FIXED_CHANCE)
                .rewardChance(new BigDecimal(chance))
                .build();
    }

    @Test
    void typeIsFixedChance() {
        assertThat(strategy.type()).isEqualTo(RewardType.FIXED_CHANCE);
    }

    @Test
    void returnsConfiguredChance() {
        assertThat(strategy.winChance(jackpot("0.10", "1000"))).isEqualTo(0.10);
    }

    @Test
    void chanceDoesNotDependOnPoolSize() {
        assertThat(strategy.winChance(jackpot("0.10", "999999"))).isEqualTo(0.10);
    }
}
