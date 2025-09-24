package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token;

/**
 * Implements a match.
 * @param patternStart the index of the start inside the pattern
 * @param searchStart the index of the start inside the search
 * @param frameLength the length of the match
 *
 * @author Programmieren-Team
 */
public record Match(int patternStart, int searchStart, int frameLength) {
}
