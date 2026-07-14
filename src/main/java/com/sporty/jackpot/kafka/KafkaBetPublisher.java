package com.sporty.jackpot.kafka;

import com.sporty.jackpot.domain.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes bets as JSON to the {@code jackpot-bets} Kafka topic. */
@Component
@Profile("kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaBetPublisher implements BetPublisher {

    public static final String TOPIC = "jackpot-bets";

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Override
    public void publish(Bet bet) {
        log.info("[kafka] Publishing bet '{}' to topic '{}'", bet.betId(), TOPIC);
        kafkaTemplate.send(TOPIC, bet.betId(), bet);
    }
}
