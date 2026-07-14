package com.sporty.jackpot.kafka;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.service.ContributionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MockBetPublisherTest {

    @Mock
    private ContributionService contributionService;

    @InjectMocks
    private MockBetPublisher publisher;

    @Test
    void publishingTriggersContributionProcessing() {
        Bet bet = new Bet("bet-1", "user-1", "jackpot-fixed", new BigDecimal("100"));

        publisher.publish(bet);

        verify(contributionService).processBet(bet);
    }
}
