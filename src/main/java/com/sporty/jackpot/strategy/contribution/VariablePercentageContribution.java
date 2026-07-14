package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Contribution percentage starts high and decays linearly as the pool grows above its initial size,
 * never dropping below the configured floor:
 * {@code pct = max(floorPct, startPct - decayRate * (currentPool - initialPool))}.
 */
@Component
public class VariablePercentageContribution implements ContributionStrategy {

    @Override
    public ContributionType type() {
        return ContributionType.VARIABLE_PERCENTAGE;
    }

    @Override
    public BigDecimal calculate(BigDecimal betAmount, Jackpot jackpot) {
        BigDecimal poolGrowth = jackpot.getCurrentPool().subtract(jackpot.getInitialPool())
                .max(BigDecimal.ZERO);
        BigDecimal percentage = jackpot.getContributionPercentage()
                .subtract(jackpot.getContributionDecayRate().multiply(poolGrowth))
                .max(jackpot.getContributionFloorPercentage());
        return betAmount.multiply(percentage)
                .setScale(FixedPercentageContribution.MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
