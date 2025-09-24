package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements the analysis of two sequences.
 *
 * @author Programmieren-Team
 */
public class SequenceAnalysis {

    private final Sequence sequence;
    private final Sequence other;
    private final Sequence searchSequence;
    private final Sequence patternSequence;
    private final int mml;
    private final List<Match> matches = new LinkedList<>();
    private final Collection<Integer> matchedSearchTokens;
    private final Collection<Integer> matchedPatternTokens;
    private final Collection<Integer> searchedSearchTokens;
    private final Collection<Integer> searchedPatternTokens;
    private final double exponent;
    private int frameLength;
    private double score;

    /**
     * Creates a new analysis between two sequences given a minimum match length.
     * @param sequence the first sequence.
     * @param other the second sequence.
     * @param mml the minimum match length.
     */
    public SequenceAnalysis(Sequence sequence, Sequence other, int mml, double exponent) {
        this.sequence = sequence;
        this.other = other;
        this.exponent = exponent;
        if (sequence.compareTo(other) < 0) {
            this.searchSequence = sequence;
            this.patternSequence = other;
        } else {
            this.searchSequence = other;
            this.patternSequence = sequence;
        }
        this.mml = mml;
        this.matchedSearchTokens = new HashSet<>();
        this.matchedPatternTokens = new HashSet<>();
        this.searchedPatternTokens = new HashSet<>();
        this.searchedSearchTokens = new HashSet<>(this.searchSequence.getLength());
        this.frameLength = this.patternSequence.getLength();
    }

    public double compareTokens() {
        while (this.frameLength >= this.mml) {
            for (int patternStartIndex = 0; patternStartIndex <= this.patternSequence.getLength() - this.frameLength; patternStartIndex++) {
                for (int searchStartIndex = 0; searchStartIndex <= this.searchSequence.getLength() - this.frameLength; searchStartIndex++) {
                    this.searchedSearchTokens.clear();
                    this.searchedPatternTokens.clear();
                    compareFrame(searchStartIndex, patternStartIndex);
                }
            }
            this.frameLength--;
        }
        return this.score;
    }

    private void compareFrame(int searchStartIndex, int patternStartIndex) {
        for (int i = 0; i < this.frameLength; i++) {
            if (this.matchedSearchTokens.contains(searchStartIndex + i)
                    || this.matchedPatternTokens.contains(patternStartIndex + i)
                    || !this.searchSequence.get(searchStartIndex + i).equalsIgnoreCase(this.patternSequence.get(patternStartIndex + i))) {
                return;
            }
            this.searchedSearchTokens.add(searchStartIndex + i);
            this.searchedPatternTokens.add(patternStartIndex + i);
        }
        this.score += Math.pow(this.frameLength, exponent);
        this.matches.add(new Match(patternStartIndex, searchStartIndex, this.frameLength));
        this.matchedSearchTokens.addAll(this.searchedSearchTokens);
        this.matchedPatternTokens.addAll(this.searchedPatternTokens);
    }

    /**
     * Returns the search sequence of this analysis.
     * @return the search sequence.
     */
    public Sequence getSearchSequence() {
        return this.searchSequence;
    }

    /**
     * Returns the pattern sequence of this analysis.
     * @return the pattern sequence.
     */
    public Sequence getPatternSequence() {
        return this.patternSequence;
    }

    /**
     * Returns the list of matches in this analysis.
     * @return the list of matches.
     */
    public List<Match> getMatches() {
        return Collections.unmodifiableList(this.matches);
    }

    /**
     * Returns the number of matched tokens.
     * @return the number of matched tokens.
     */
    public int getMatchedTokenCount() {
        return this.matches.size();
    }
    
    public double getScore() {
        return this.score;
    }

    public Sequence getSequence() {
        return sequence;
    }

    public Sequence getOther() {
        return other;
    }
}
