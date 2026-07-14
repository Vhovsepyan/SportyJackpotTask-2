package com.sporty.jackpot.api;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.kafka.BetPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BetController.class)
class BetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BetPublisher betPublisher;

    @Test
    void validBetIsAcceptedAndPublished() throws Exception {
        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"betId":"bet-1","userId":"user-1","jackpotId":"jackpot-fixed","amount":100.50}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.betId").value("bet-1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(betPublisher).publish(new Bet("bet-1", "user-1", "jackpot-fixed", new BigDecimal("100.50")));
    }

    @Test
    void missingFieldsReturn400WithDetails() throws Exception {
        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"betId":"","amount":100}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[?(@ =~ /betId.*/)]").exists())
                .andExpect(jsonPath("$.details[?(@ =~ /jackpotId.*/)]").exists())
                .andExpect(jsonPath("$.details[?(@ =~ /userId.*/)]").exists());

        verify(betPublisher, never()).publish(any());
    }

    @Test
    void nonPositiveAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"betId":"bet-1","userId":"user-1","jackpotId":"jackpot-fixed","amount":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@ =~ /amount.*/)]").exists());

        verify(betPublisher, never()).publish(any());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(betPublisher, never()).publish(any());
    }
}
