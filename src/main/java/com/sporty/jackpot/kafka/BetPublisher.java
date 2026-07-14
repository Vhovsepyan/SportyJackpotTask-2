package com.sporty.jackpot.kafka;

import com.sporty.jackpot.domain.Bet;

/** Publishes a bet for asynchronous jackpot contribution processing. */
public interface BetPublisher {

    void publish(Bet bet);
}
