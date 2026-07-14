package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JackpotRepositoryTest {

    @Autowired
    private JackpotRepository jackpotRepository;

    @Test
    void saveAndFindRoundTrip() {
        Jackpot jackpot = Jackpot.builder()
                .id("jackpot-test")
                .initialPool(new BigDecimal("1000.0000"))
                .currentPool(new BigDecimal("1250.5000"))
                .contributionType(ContributionType.FIXED_PERCENTAGE)
                .contributionPercentage(new BigDecimal("0.050000"))
                .rewardType(RewardType.VARIABLE_CHANCE)
                .rewardChance(new BigDecimal("0.010000"))
                .rewardPoolLimit(new BigDecimal("10000.0000"))
                .build();

        jackpotRepository.saveAndFlush(jackpot);

        Optional<Jackpot> found = jackpotRepository.findById("jackpot-test");
        assertThat(found).isPresent();
        assertThat(found.get().getInitialPool()).isEqualByComparingTo("1000");
        assertThat(found.get().getCurrentPool()).isEqualByComparingTo("1250.50");
        assertThat(found.get().getContributionType()).isEqualTo(ContributionType.FIXED_PERCENTAGE);
        assertThat(found.get().getContributionPercentage()).isEqualByComparingTo("0.05");
        assertThat(found.get().getRewardType()).isEqualTo(RewardType.VARIABLE_CHANCE);
        assertThat(found.get().getRewardPoolLimit()).isEqualByComparingTo("10000");
        assertThat(found.get().getVersion()).isNotNull();
    }
}
