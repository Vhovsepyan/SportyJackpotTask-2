package com.sporty.jackpot.api;

import com.sporty.jackpot.kafka.BetPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetPublisher betPublisher;

    /** Accepts a bet and publishes it for asynchronous jackpot contribution processing. */
    @PostMapping
    public ResponseEntity<Map<String, String>> publishBet(@Valid @RequestBody BetRequest request) {
        betPublisher.publish(request.toBet());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("betId", request.betId(), "status", "ACCEPTED"));
    }
}
