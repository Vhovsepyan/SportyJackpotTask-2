package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.strategy.reward.FixedChanceReward;
import com.sporty.jackpot.strategy.reward.RewardStrategy;
import com.sporty.jackpot.strategy.reward.VariableChanceReward;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class RewardStrategyFactoryTest {

    private final RewardStrategyFactory factory = new RewardStrategyFactory(
            List.of(new FixedChanceReward(), new VariableChanceReward()));

    @Test
    void resolvesEveryRewardType() {
        for (RewardType type : RewardType.values()) {
            RewardStrategy strategy = factory.forType(type);
            assertThat(strategy.type()).isEqualTo(type);
        }
    }

    @Test
    void throwsWhenNoStrategyRegisteredForType() {
        RewardStrategyFactory partial = new RewardStrategyFactory(List.of(new FixedChanceReward()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> partial.forType(RewardType.VARIABLE_CHANCE))
                .withMessageContaining("VARIABLE_CHANCE");
    }

    @Test
    void rejectsDuplicateStrategiesForSameType() {
        assertThatIllegalStateException().isThrownBy(() -> new RewardStrategyFactory(
                List.of(new FixedChanceReward(), new FixedChanceReward())));
    }
}
