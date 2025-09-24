package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.Arrays;

/**
 * Implements the sequence.
 *
 * @author Programmieren-Team
 */
public class Sequence implements Comparable<Sequence> {

    private final Element element;
    private final String[] tokens;

    /**
     * Creates a new sequence.
     * @param element the identifier of the text.
     * @param tokens the tokens of the text.
     */
    public Sequence(Element element, String[] tokens) {
        this.element = element;
        this.tokens = tokens.clone();
    }

    /**
     * Returns the text identifier.
     * @return the text identifier.
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the token at a given index.
     * @param index the index.
     * @return the token at the index.
     */
    public String get(int index) {
        return this.tokens[index];
    }

    /**
     * Returns all tokens.
     * @return the tokens.
     */
    public String[] getTokens() {
        return this.tokens.clone();
    }

    /**
     * Returns the number of tokens this sequence has.
     * @return the number of tokens.
     */
    public int getLength() {
        return this.tokens.length;
    }

    @Override
    public int compareTo(Sequence other) {
        return Integer.compare(other.tokens.length, this.tokens.length);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Sequence sequence = (Sequence) o;
        return Arrays.equals(tokens, sequence.tokens);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(tokens);
    }
}
