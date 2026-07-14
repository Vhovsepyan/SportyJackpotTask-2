package com.sporty.jackpot.api;

import com.sporty.jackpot.service.RewardResult;
import com.sporty.jackpot.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    /** Evaluates a previously placed bet for a jackpot reward. Idempotent for winning bets. */
    @PostMapping("/{betId}/evaluate")
    public RewardResult evaluate(@PathVariable String betId) {
        return rewardService.evaluate(betId);
    }
}
