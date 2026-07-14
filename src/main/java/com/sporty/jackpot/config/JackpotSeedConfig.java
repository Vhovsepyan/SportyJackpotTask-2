package com.sporty.jackpot.config;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.repository.JackpotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/** Seeds two demo jackpots on startup so the API is usable out of the box. */
@Configuration
@Slf4j
public class JackpotSeedConfig {

    public static final String FIXED_JACKPOT_ID = "jackpot-fixed";
    public static final String VARIABLE_JACKPOT_ID = "jackpot-variable";

    @Bean
    CommandLineRunner seedJackpots(JackpotRepository jackpotRepository) {
        return args -> {
            if (!jackpotRepository.existsById(FIXED_JACKPOT_ID)) {
                jackpotRepository.save(Jackpot.builder()
                        .id(FIXED_JACKPOT_ID)
                        .initialPool(new BigDecimal("1000.00"))
                        .currentPool(new BigDecimal("1000.00"))
                        .contributionType(ContributionType.FIXED_PERCENTAGE)
                        .contributionPercentage(new BigDecimal("0.05"))
                        .rewardType(RewardType.FIXED_CHANCE)
                        .rewardChance(new BigDecimal("0.10"))
                        .build());
            }
            if (!jackpotRepository.existsById(VARIABLE_JACKPOT_ID)) {
                jackpotRepository.save(Jackpot.builder()
                        .id(VARIABLE_JACKPOT_ID)
                        .initialPool(new BigDecimal("5000.00"))
                        .currentPool(new BigDecimal("5000.00"))
                        .contributionType(ContributionType.VARIABLE_PERCENTAGE)
                        .contributionPercentage(new BigDecimal("0.10"))
                        .contributionDecayRate(new BigDecimal("0.00001"))
                        .contributionFloorPercentage(new BigDecimal("0.02"))
                        .rewardType(RewardType.VARIABLE_CHANCE)
                        .rewardChance(new BigDecimal("0.01"))
                        .rewardPoolLimit(new BigDecimal("10000.00"))
                        .build());
            }
            log.info("Seeded jackpots: '{}' (FIXED_PERCENTAGE contribution / FIXED_CHANCE reward), "
                            + "'{}' (VARIABLE_PERCENTAGE contribution / VARIABLE_CHANCE reward)",
                    FIXED_JACKPOT_ID, VARIABLE_JACKPOT_ID);
        };
    }
}
