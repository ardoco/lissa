/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.utils;

/**
 * A utility record class representing an immutable pair of values.
 *
 * @param <F> The type of the first value in the pair
 * @param <S> The type of the second value in the pair
 */
public record Pair<F, S>(F first, S second) {}
