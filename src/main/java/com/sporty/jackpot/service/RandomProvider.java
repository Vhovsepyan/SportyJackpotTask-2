package com.sporty.jackpot.service;

/** Source of randomness for reward rolls, injectable so tests can be deterministic. */
public interface RandomProvider {

    /** Returns a uniformly distributed value in [0.0, 1.0). */
    double nextDouble();
}
