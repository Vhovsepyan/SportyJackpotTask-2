package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.strategy.ContributionStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Processes incoming bets: calculates the jackpot contribution, grows the pool and
 * persists a {@link JackpotContribution} snapshot. Optimistic locking on {@link Jackpot}
 * (via {@code @Version}) protects concurrent pool updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContributionService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final ContributionStrategyFactory contributionStrategyFactory;

    @Transactional
    public void processBet(Bet bet) {
        Jackpot jackpot = jackpotRepository.findById(bet.jackpotId()).orElse(null);
        if (jackpot == null) {
            log.warn("Skipping bet '{}': unknown jackpot '{}'", bet.betId(), bet.jackpotId());
            return;
        }

        BigDecimal contribution = contributionStrategyFactory
                .forType(jackpot.getContributionType())
                .calculate(bet.amount(), jackpot);

        jackpot.setCurrentPool(jackpot.getCurrentPool().add(contribution));
        jackpotRepository.save(jackpot);

        contributionRepository.save(JackpotContribution.builder()
                .betId(bet.betId())
                .userId(bet.userId())
                .jackpotId(jackpot.getId())
                .stakeAmount(bet.amount())
                .contributionAmount(contribution)
                .currentJackpotAmount(jackpot.getCurrentPool())
                .build());

        log.info("Bet '{}' contributed {} to jackpot '{}' (pool now {})",
                bet.betId(), contribution, jackpot.getId(), jackpot.getCurrentPool());
    }
}
