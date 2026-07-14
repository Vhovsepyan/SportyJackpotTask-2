package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.strategy.contribution.ContributionStrategy;
import com.sporty.jackpot.strategy.contribution.FixedPercentageContribution;
import com.sporty.jackpot.strategy.contribution.VariablePercentageContribution;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ContributionStrategyFactoryTest {

    private final ContributionStrategyFactory factory = new ContributionStrategyFactory(
            List.of(new FixedPercentageContribution(), new VariablePercentageContribution()));

    @Test
    void resolvesEveryContributionType() {
        for (ContributionType type : ContributionType.values()) {
            ContributionStrategy strategy = factory.forType(type);
            assertThat(strategy.type()).isEqualTo(type);
        }
    }

    @Test
    void throwsWhenNoStrategyRegisteredForType() {
        ContributionStrategyFactory partial = new ContributionStrategyFactory(
                List.of(new FixedPercentageContribution()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> partial.forType(ContributionType.VARIABLE_PERCENTAGE))
                .withMessageContaining("VARIABLE_PERCENTAGE");
    }

    @Test
    void rejectsDuplicateStrategiesForSameType() {
        assertThatIllegalStateException().isThrownBy(() -> new ContributionStrategyFactory(
                List.of(new FixedPercentageContribution(), new FixedPercentageContribution())));
    }
}
