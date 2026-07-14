package com.sporty.jackpot.api;

import com.sporty.jackpot.service.ResourceNotFoundException;
import com.sporty.jackpot.service.RewardResult;
import com.sporty.jackpot.service.RewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RewardController.class)
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardService rewardService;

    @Test
    void winningEvaluationReturnsReward() throws Exception {
        when(rewardService.evaluate("bet-1"))
                .thenReturn(RewardResult.won("bet-1", new BigDecimal("1500.00")));

        mockMvc.perform(post("/api/bets/bet-1/evaluate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value("bet-1"))
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").value(1500.00));
    }

    @Test
    void losingEvaluationReturnsNoWin() throws Exception {
        when(rewardService.evaluate("bet-2")).thenReturn(RewardResult.lost("bet-2"));

        mockMvc.perform(post("/api/bets/bet-2/evaluate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value("bet-2"))
                .andExpect(jsonPath("$.won").value(false))
                .andExpect(jsonPath("$.rewardAmount").isEmpty());
    }

    @Test
    void unknownBetReturns404() throws Exception {
        when(rewardService.evaluate("no-such-bet"))
                .thenThrow(new ResourceNotFoundException("Bet 'no-such-bet' has no jackpot contribution"));

        mockMvc.perform(post("/api/bets/no-such-bet/evaluate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Bet 'no-such-bet' has no jackpot contribution"));
    }
}
