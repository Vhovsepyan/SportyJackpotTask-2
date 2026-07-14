package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.springframework.stereotype.Component;

/** Win chance is a constant percentage regardless of pool size. */
@Component
public class FixedChanceReward implements RewardStrategy {

    @Override
    public RewardType type() {
        return RewardType.FIXED_CHANCE;
    }

    @Override
    public double winChance(Jackpot jackpot) {
        return jackpot.getRewardChance().doubleValue();
    }
}
