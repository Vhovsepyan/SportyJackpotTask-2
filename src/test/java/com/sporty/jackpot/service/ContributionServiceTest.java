package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.strategy.ContributionStrategyFactory;
import com.sporty.jackpot.strategy.contribution.FixedPercentageContribution;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotContributionRepository contributionRepository;

    @Mock
    private ContributionStrategyFactory contributionStrategyFactory;

    @InjectMocks
    private ContributionService contributionService;

    private Jackpot jackpot;

    @BeforeEach
    void setUp() {
        jackpot = Jackpot.builder()
                .id("jackpot-fixed")
                .initialPool(new BigDecimal("1000"))
                .currentPool(new BigDecimal("1000"))
                .contributionType(ContributionType.FIXED_PERCENTAGE)
                .contributionPercentage(new BigDecimal("0.05"))
                .rewardType(RewardType.FIXED_CHANCE)
                .rewardChance(new BigDecimal("0.10"))
                .build();
        lenient().when(contributionStrategyFactory.forType(ContributionType.FIXED_PERCENTAGE))
                .thenReturn(new FixedPercentageContribution());
    }

    @Test
    void addsContributionToPoolAndSavesJackpot() {
        when(jackpotRepository.findById("jackpot-fixed")).thenReturn(Optional.of(jackpot));

        contributionService.processBet(new Bet("bet-1", "user-1", "jackpot-fixed", new BigDecimal("100")));

        ArgumentCaptor<Jackpot> saved = ArgumentCaptor.forClass(Jackpot.class);
        verify(jackpotRepository).save(saved.capture());
        assertThat(saved.getValue().getCurrentPool()).isEqualByComparingTo("1005"); // 1000 + 5% of 100
    }

    @Test
    void persistsContributionSnapshot() {
        when(jackpotRepository.findById("jackpot-fixed")).thenReturn(Optional.of(jackpot));

        contributionService.processBet(new Bet("bet-1", "user-1", "jackpot-fixed", new BigDecimal("100")));

        ArgumentCaptor<JackpotContribution> saved = ArgumentCaptor.forClass(JackpotContribution.class);
        verify(contributionRepository).save(saved.capture());
        JackpotContribution contribution = saved.getValue();
        assertThat(contribution.getBetId()).isEqualTo("bet-1");
        assertThat(contribution.getUserId()).isEqualTo("user-1");
        assertThat(contribution.getJackpotId()).isEqualTo("jackpot-fixed");
        assertThat(contribution.getStakeAmount()).isEqualByComparingTo("100");
        assertThat(contribution.getContributionAmount()).isEqualByComparingTo("5");
        assertThat(contribution.getCurrentJackpotAmount()).isEqualByComparingTo("1005");
        assertThat(contribution.getCreatedAt()).isNotNull();
    }

    @Test
    void skipsUnknownJackpotWithoutError() {
        when(jackpotRepository.findById("no-such-jackpot")).thenReturn(Optional.empty());

        contributionService.processBet(new Bet("bet-1", "user-1", "no-such-jackpot", new BigDecimal("100")));

        verify(jackpotRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(contributionRepository);
    }
}
