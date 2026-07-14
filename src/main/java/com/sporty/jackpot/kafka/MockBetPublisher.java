package com.sporty.jackpot.kafka;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.service.ContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default publisher: no broker needed. Logs the payload and hands the bet straight
 * to the same processing path the Kafka consumer would use.
 */
@Component
@Profile("mock")
@RequiredArgsConstructor
@Slf4j
public class MockBetPublisher implements BetPublisher {

    private final ContributionService contributionService;

    @Override
    public void publish(Bet bet) {
        log.info("[mock] Publishing bet: {}", bet);
        contributionService.processBet(bet);
    }
}
