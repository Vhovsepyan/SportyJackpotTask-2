package com.sporty.jackpot.strategy;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.strategy.contribution.ContributionStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the {@link ContributionStrategy} for a jackpot's configured contribution type. */
@Component
public class ContributionStrategyFactory {

    private final Map<ContributionType, ContributionStrategy> strategies;

    public ContributionStrategyFactory(List<ContributionStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toMap(
                ContributionStrategy::type,
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Duplicate contribution strategy for type " + a.type());
                },
                () -> new EnumMap<>(ContributionType.class)));
    }

    public ContributionStrategy forType(ContributionType type) {
        ContributionStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No contribution strategy registered for type: " + type);
        }
        return strategy;
    }
}
