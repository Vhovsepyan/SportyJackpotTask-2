package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.Jackpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JackpotRepository extends JpaRepository<Jackpot, String> {
}
