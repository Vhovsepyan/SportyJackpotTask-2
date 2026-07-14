package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.domain.JackpotReward;
import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.repository.JackpotRewardRepository;
import com.sporty.jackpot.strategy.RewardStrategyFactory;
import com.sporty.jackpot.strategy.reward.FixedChanceReward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotContributionRepository contributionRepository;

    @Mock
    private JackpotRewardRepository rewardRepository;

    @Mock
    private RewardStrategyFactory rewardStrategyFactory;

    @Mock
    private RandomProvider randomProvider;

    @InjectMocks
    private RewardService rewardService;

    private Jackpot jackpot;
    private JackpotContribution contribution;

    @BeforeEach
    void setUp() {
        jackpot = Jackpot.builder()
                .id("jackpot-fixed")
                .initialPool(new BigDecimal("1000"))
                .currentPool(new BigDecimal("1500"))
                .contributionType(ContributionType.FIXED_PERCENTAGE)
                .contributionPercentage(new BigDecimal("0.05"))
                .rewardType(RewardType.FIXED_CHANCE)
                .rewardChance(new BigDecimal("0.10"))
                .build();
        contribution = JackpotContribution.builder()
                .betId("bet-1")
                .userId("user-1")
                .jackpotId("jackpot-fixed")
                .stakeAmount(new BigDecimal("100"))
                .contributionAmount(new BigDecimal("5"))
                .currentJackpotAmount(new BigDecimal("1500"))
                .build();
        lenient().when(rewardRepository.findByBetId("bet-1")).thenReturn(Optional.empty());
        lenient().when(contributionRepository.findByBetId("bet-1")).thenReturn(Optional.of(contribution));
        lenient().when(jackpotRepository.findById("jackpot-fixed")).thenReturn(Optional.of(jackpot));
        lenient().when(rewardStrategyFactory.forType(RewardType.FIXED_CHANCE))
                .thenReturn(new FixedChanceReward());
    }

    @Test
    void winPersistsRewardAndResetsPool() {
        when(randomProvider.nextDouble()).thenReturn(0.05); // below 0.10 chance -> win

        RewardResult result = rewardService.evaluate("bet-1");

        assertThat(result.won()).isTrue();
        assertThat(result.rewardAmount()).isEqualByComparingTo("1500");

        ArgumentCaptor<JackpotReward> rewardCaptor = ArgumentCaptor.forClass(JackpotReward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        JackpotReward reward = rewardCaptor.getValue();
        assertThat(reward.getBetId()).isEqualTo("bet-1");
        assertThat(reward.getUserId()).isEqualTo("user-1");
        assertThat(reward.getJackpotId()).isEqualTo("jackpot-fixed");
        assertThat(reward.getJackpotRewardAmount()).isEqualByComparingTo("1500");

        ArgumentCaptor<Jackpot> jackpotCaptor = ArgumentCaptor.forClass(Jackpot.class);
        verify(jackpotRepository).save(jackpotCaptor.capture());
        assertThat(jackpotCaptor.getValue().getCurrentPool()).isEqualByComparingTo("1000");
    }

    @Test
    void lossChangesNothing() {
        when(randomProvider.nextDouble()).thenReturn(0.95); // above 0.10 chance -> loss

        RewardResult result = rewardService.evaluate("bet-1");

        assertThat(result.won()).isFalse();
        assertThat(result.rewardAmount()).isNull();
        verify(rewardRepository, never()).save(any());
        verify(jackpotRepository, never()).save(any());
        assertThat(jackpot.getCurrentPool()).isEqualByComparingTo("1500");
    }

    @Test
    void repeatedEvaluationReturnsExistingRewardWithoutRollingAgain() {
        when(rewardRepository.findByBetId("bet-1")).thenReturn(Optional.of(JackpotReward.builder()
                .betId("bet-1")
                .userId("user-1")
                .jackpotId("jackpot-fixed")
                .jackpotRewardAmount(new BigDecimal("1500"))
                .build()));

        RewardResult result = rewardService.evaluate("bet-1");

        assertThat(result.won()).isTrue();
        assertThat(result.rewardAmount()).isEqualByComparingTo("1500");
        verify(rewardRepository, never()).save(any());
        verify(jackpotRepository, never()).save(any());
        verify(randomProvider, never()).nextDouble();
    }

    @Test
    void unknownBetThrowsNotFound() {
        when(contributionRepository.findByBetId("no-such-bet")).thenReturn(Optional.empty());
        when(rewardRepository.findByBetId("no-such-bet")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> rewardService.evaluate("no-such-bet"))
                .withMessageContaining("no-such-bet");
    }
}
