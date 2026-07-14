package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.domain.JackpotReward;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.repository.JackpotRewardRepository;
import com.sporty.jackpot.strategy.RewardStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Evaluates bets for jackpot rewards. Evaluation is idempotent per bet: once a bet has won,
 * repeated calls return the recorded reward instead of rolling again.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final JackpotRewardRepository rewardRepository;
    private final RewardStrategyFactory rewardStrategyFactory;
    private final RandomProvider randomProvider;

    @Transactional
    public RewardResult evaluate(String betId) {
        JackpotReward existing = rewardRepository.findByBetId(betId).orElse(null);
        if (existing != null) {
            return RewardResult.won(betId, existing.getJackpotRewardAmount());
        }

        JackpotContribution contribution = contributionRepository.findByBetId(betId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bet '" + betId + "' has no jackpot contribution"));

        Jackpot jackpot = jackpotRepository.findById(contribution.getJackpotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jackpot '" + contribution.getJackpotId() + "' not found"));

        double winChance = rewardStrategyFactory.forType(jackpot.getRewardType()).winChance(jackpot);
        double roll = randomProvider.nextDouble();
        if (roll >= winChance) {
            log.info("Bet '{}' did not win jackpot '{}' (chance {}, roll {})",
                    betId, jackpot.getId(), winChance, roll);
            return RewardResult.lost(betId);
        }

        BigDecimal rewardAmount = jackpot.getCurrentPool();
        rewardRepository.save(JackpotReward.builder()
                .betId(betId)
                .userId(contribution.getUserId())
                .jackpotId(jackpot.getId())
                .jackpotRewardAmount(rewardAmount)
                .build());

        jackpot.setCurrentPool(jackpot.getInitialPool());
        jackpotRepository.save(jackpot);

        log.info("Bet '{}' WON jackpot '{}': {} (pool reset to {})",
                betId, jackpot.getId(), rewardAmount, jackpot.getInitialPool());
        return RewardResult.won(betId, rewardAmount);
    }
}
