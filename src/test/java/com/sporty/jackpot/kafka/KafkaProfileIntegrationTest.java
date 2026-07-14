package com.sporty.jackpot.kafka;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the real Kafka publisher/consumer pair against an embedded broker. */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@ActiveProfiles("kafka")
@EmbeddedKafka(partitions = 1, topics = KafkaBetPublisher.TOPIC)
class KafkaProfileIntegrationTest {

    @Autowired
    private BetPublisher betPublisher;

    @Autowired
    private JackpotContributionRepository contributionRepository;

    @Test
    void publishedBetIsConsumedAndContributed() {
        assertThat(betPublisher).isInstanceOf(KafkaBetPublisher.class);

        betPublisher.publish(new Bet("kafka-bet-1", "user-1", "jackpot-fixed", new BigDecimal("100")));

        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (contributionRepository.findByBetId("kafka-bet-1").isEmpty()
                && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertThat(contributionRepository.findByBetId("kafka-bet-1"))
                .as("bet should be consumed from Kafka and contributed within 30s")
                .hasValueSatisfying(contribution ->
                        assertThat(contribution.getContributionAmount()).isEqualByComparingTo("5"));
    }
}
