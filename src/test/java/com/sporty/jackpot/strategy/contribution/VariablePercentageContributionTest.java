package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VariablePercentageContributionTest {

    private final VariablePercentageContribution strategy = new VariablePercentageContribution();

    /** startPct 10%, decay 0.00001 per pool unit above initial (5000), floor 2%. */
    private Jackpot jackpot(String currentPool) {
        return Jackpot.builder()
                .id("j2")
                .initialPool(new BigDecimal("5000"))
                .currentPool(new BigDecimal(currentPool))
                .contributionType(ContributionType.VARIABLE_PERCENTAGE)
                .contributionPercentage(new BigDecimal("0.10"))
                .contributionDecayRate(new BigDecimal("0.00001"))
                .contributionFloorPercentage(new BigDecimal("0.02"))
                .build();
    }

    @Test
    void typeIsVariablePercentage() {
        assertThat(strategy.type()).isEqualTo(ContributionType.VARIABLE_PERCENTAGE);
    }

    @Test
    void usesStartPercentageWhenPoolAtInitial() {
        BigDecimal result = strategy.calculate(new BigDecimal("200"), jackpot("5000"));
        assertThat(result).isEqualByComparingTo("20"); // 200 * 0.10
    }

    @Test
    void percentageDecaysAsPoolGrows() {
        // growth 2000 -> pct = 0.10 - 0.00001 * 2000 = 0.08
        BigDecimal result = strategy.calculate(new BigDecimal("200"), jackpot("7000"));
        assertThat(result).isEqualByComparingTo("16");
    }

    @Test
    void percentageKeepsDecreasingWithMoreGrowth() {
        // growth 5000 -> pct = 0.10 - 0.05 = 0.05
        BigDecimal result = strategy.calculate(new BigDecimal("200"), jackpot("10000"));
        assertThat(result).isEqualByComparingTo("10");
    }

    @Test
    void floorIsRespected() {
        // growth 20000 -> raw pct = 0.10 - 0.20 = -0.10 -> clamped to floor 0.02
        BigDecimal result = strategy.calculate(new BigDecimal("200"), jackpot("25000"));
        assertThat(result).isEqualByComparingTo("4");
    }

    @Test
    void poolBelowInitialUsesStartPercentage() {
        BigDecimal result = strategy.calculate(new BigDecimal("200"), jackpot("3000"));
        assertThat(result).isEqualByComparingTo("20");
    }
}
