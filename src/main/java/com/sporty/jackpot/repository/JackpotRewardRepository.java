package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JackpotRewardRepository extends JpaRepository<JackpotReward, String> {

    Optional<JackpotReward> findByBetId(String betId);
}
