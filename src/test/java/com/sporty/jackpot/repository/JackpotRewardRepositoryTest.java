package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotReward;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JackpotRewardRepositoryTest {

    @Autowired
    private JackpotRewardRepository rewardRepository;

    @Test
    void saveAndFindByBetId() {
        JackpotReward reward = JackpotReward.builder()
                .betId("bet-777")
                .userId("user-9")
                .jackpotId("jackpot-variable")
                .jackpotRewardAmount(new BigDecimal("8250.7500"))
                .build();

        rewardRepository.saveAndFlush(reward);

        Optional<JackpotReward> found = rewardRepository.findByBetId("bet-777");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotBlank();
        assertThat(found.get().getUserId()).isEqualTo("user-9");
        assertThat(found.get().getJackpotId()).isEqualTo("jackpot-variable");
        assertThat(found.get().getJackpotRewardAmount()).isEqualByComparingTo("8250.75");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByBetIdReturnsEmptyForUnknownBet() {
        assertThat(rewardRepository.findByBetId("no-such-bet")).isEmpty();
    }
}
