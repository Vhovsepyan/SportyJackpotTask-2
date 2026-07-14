package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Win chance starts low and grows linearly with the pool: it is the configured start chance
 * at the initial pool, interpolates towards 1.0 as the pool approaches the pool limit,
 * and is exactly 1.0 once the pool reaches or exceeds the limit.
 */
@Component
public class VariableChanceReward implements RewardStrategy {

    @Override
    public RewardType type() {
        return RewardType.VARIABLE_CHANCE;
    }

    @Override
    public double winChance(Jackpot jackpot) {
        BigDecimal pool = jackpot.getCurrentPool();
        BigDecimal initial = jackpot.getInitialPool();
        BigDecimal limit = jackpot.getRewardPoolLimit();
        double startChance = jackpot.getRewardChance().doubleValue();

        if (pool.compareTo(limit) >= 0) {
            return 1.0;
        }
        if (pool.compareTo(initial) <= 0) {
            return startChance;
        }
        double progress = pool.subtract(initial).doubleValue() / limit.subtract(initial).doubleValue();
        return startChance + (1.0 - startChance) * progress;
    }
}
