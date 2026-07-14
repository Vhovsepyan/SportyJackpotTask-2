package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotContribution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JackpotContributionRepositoryTest {

    @Autowired
    private JackpotContributionRepository contributionRepository;

    @Test
    void saveAndFindByBetId() {
        JackpotContribution contribution = JackpotContribution.builder()
                .betId("bet-123")
                .userId("user-1")
                .jackpotId("jackpot-fixed")
                .stakeAmount(new BigDecimal("100.0000"))
                .contributionAmount(new BigDecimal("5.0000"))
                .currentJackpotAmount(new BigDecimal("1005.0000"))
                .build();

        contributionRepository.saveAndFlush(contribution);

        Optional<JackpotContribution> found = contributionRepository.findByBetId("bet-123");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotBlank();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
        assertThat(found.get().getJackpotId()).isEqualTo("jackpot-fixed");
        assertThat(found.get().getStakeAmount()).isEqualByComparingTo("100");
        assertThat(found.get().getContributionAmount()).isEqualByComparingTo("5");
        assertThat(found.get().getCurrentJackpotAmount()).isEqualByComparingTo("1005");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByBetIdReturnsEmptyForUnknownBet() {
        assertThat(contributionRepository.findByBetId("no-such-bet")).isEmpty();
    }
}
