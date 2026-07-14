package com.sporty.jackpot;

import com.sporty.jackpot.config.JackpotSeedConfig;
import com.sporty.jackpot.domain.JackpotContribution;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end flow on the default mock profile: publish a bet, then evaluate it. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
class JackpotFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JackpotRepository jackpotRepository;

    @Autowired
    private JackpotContributionRepository contributionRepository;

    @Test
    void betIsContributedAndEvaluatedEndToEnd() {
        BigDecimal poolBefore = jackpotRepository.findById(JackpotSeedConfig.FIXED_JACKPOT_ID)
                .orElseThrow().getCurrentPool();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> publishResponse = restTemplate.postForEntity("/api/bets",
                new HttpEntity<>("""
                        {"betId":"it-bet-1","userId":"it-user-1","jackpotId":"jackpot-fixed","amount":200}
                        """, headers),
                Map.class);

        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(publishResponse.getBody()).containsEntry("betId", "it-bet-1");

        // contribution recorded (mock publisher processes synchronously): 5% of 200 = 10
        JackpotContribution contribution = contributionRepository.findByBetId("it-bet-1").orElseThrow();
        assertThat(contribution.getContributionAmount()).isEqualByComparingTo("10");
        assertThat(contribution.getCurrentJackpotAmount()).isEqualByComparingTo(poolBefore.add(BigDecimal.TEN));

        // pool increased
        BigDecimal poolAfter = jackpotRepository.findById(JackpotSeedConfig.FIXED_JACKPOT_ID)
                .orElseThrow().getCurrentPool();
        assertThat(poolAfter).isEqualByComparingTo(poolBefore.add(BigDecimal.TEN));

        // evaluation returns a valid win/lose response
        ResponseEntity<Map> evaluateResponse = restTemplate.postForEntity(
                "/api/bets/it-bet-1/evaluate", null, Map.class);
        assertThat(evaluateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = evaluateResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("betId")).isEqualTo("it-bet-1");
        assertThat(body.get("won")).isInstanceOf(Boolean.class);
        if ((Boolean) body.get("won")) {
            assertThat(new BigDecimal(body.get("rewardAmount").toString()))
                    .isEqualByComparingTo(poolAfter);
            assertThat(jackpotRepository.findById(JackpotSeedConfig.FIXED_JACKPOT_ID)
                    .orElseThrow().getCurrentPool()).isEqualByComparingTo("1000");
        } else {
            assertThat(body.get("rewardAmount")).isNull();
        }
    }

    @Test
    void evaluatingUnknownBetReturns404() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/bets/unknown-bet/evaluate", null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
