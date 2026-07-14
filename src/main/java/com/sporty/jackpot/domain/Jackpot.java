package com.sporty.jackpot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "jackpots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jackpot {

    @Id
    private String id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal initialPool;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPool;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionType contributionType;

    /** Fixed: the flat percentage. Variable: the starting percentage. As a fraction, e.g. 0.05 = 5%. */
    @Column(precision = 10, scale = 6)
    private BigDecimal contributionPercentage;

    /** Variable only: percentage drop per unit of pool growth above the initial pool. */
    @Column(precision = 19, scale = 12)
    private BigDecimal contributionDecayRate;

    /** Variable only: the percentage never drops below this floor. */
    @Column(precision = 10, scale = 6)
    private BigDecimal contributionFloorPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType rewardType;

    /** Fixed: the flat win chance. Variable: the starting win chance. As a fraction, e.g. 0.01 = 1%. */
    @Column(precision = 10, scale = 6)
    private BigDecimal rewardChance;

    /** Variable only: pool size at which the win chance reaches 100%. */
    @Column(precision = 19, scale = 4)
    private BigDecimal rewardPoolLimit;

    @Version
    private Long version;
}
