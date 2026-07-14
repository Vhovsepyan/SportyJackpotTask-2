package com.sporty.jackpot.kafka;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.service.ContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes bets from the {@code jackpot-bets} topic and feeds them into contribution processing. */
@Component
@Profile("kafka")
@RequiredArgsConstructor
@Slf4j
public class BetConsumer {

    private final ContributionService contributionService;

    @KafkaListener(topics = KafkaBetPublisher.TOPIC)
    public void onBet(Bet bet) {
        log.info("[kafka] Received bet '{}' from topic '{}'", bet.betId(), KafkaBetPublisher.TOPIC);
        contributionService.processBet(bet);
    }
}
