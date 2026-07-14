package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.RewardType;
import com.sporty.jackpot.strategy.reward.RewardStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the {@link RewardStrategy} for a jackpot's configured reward type. */
@Component
public class RewardStrategyFactory {

    private final Map<RewardType, RewardStrategy> strategies;

    public RewardStrategyFactory(List<RewardStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(
                RewardStrategy::type,
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Duplicate reward strategy for type " + a.type());
                },
                () -> new EnumMap<>(RewardType.class)));
    }

    public RewardStrategy forType(RewardType type) {
        RewardStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No reward strategy registered for type: " + type);
        }
        return strategy;
    }
}
