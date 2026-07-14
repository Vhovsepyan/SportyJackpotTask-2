package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JackpotContributionRepository extends JpaRepository<JackpotContribution, String> {

    Optional<JackpotContribution> findByBetId(String betId);
}
