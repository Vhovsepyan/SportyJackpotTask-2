package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FixedPercentageContributionTest {

    private final FixedPercentageContribution strategy = new FixedPercentageContribution();

    private Jackpot jackpot(String percentage) {
        return Jackpot.builder()
                .id("j1")
                .initialPool(new BigDecimal("1000"))
                .currentPool(new BigDecimal("1000"))
                .contributionType(ContributionType.FIXED_PERCENTAGE)
                .contributionPercentage(new BigDecimal(percentage))
                .build();
    }

    @Test
    void typeIsFixedPercentage() {
        assertThat(strategy.type()).isEqualTo(ContributionType.FIXED_PERCENTAGE);
    }

    @Test
    void contributionIsPercentageOfBetAmount() {
        BigDecimal result = strategy.calculate(new BigDecimal("100"), jackpot("0.05"));
        assertThat(result).isEqualByComparingTo("5");
    }

    @Test
    void contributionIsRoundedToMoneyScale() {
        BigDecimal result = strategy.calculate(new BigDecimal("33.33"), jackpot("0.0333"));
        // 33.33 * 0.0333 = 1.109889 -> 1.1099
        assertThat(result).isEqualByComparingTo("1.1099");
    }

    @Test
    void contributionDoesNotDependOnPoolSize() {
        Jackpot grown = jackpot("0.05");
        grown.setCurrentPool(new BigDecimal("999999"));
        BigDecimal result = strategy.calculate(new BigDecimal("100"), grown);
        assertThat(result).isEqualByComparingTo("5");
    }
}
