package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Contribution = fixed percentage of the bet amount. */
@Component
public class FixedPercentageContribution implements ContributionStrategy {

    static final int MONEY_SCALE = 4;

    @Override
    public ContributionType type() {
        return ContributionType.FIXED_PERCENTAGE;
    }

    @Override
    public BigDecimal calculate(BigDecimal betAmount, Jackpot jackpot) {
        return betAmount.multiply(jackpot.getContributionPercentage())
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
