package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements the tokenizations.
 *
 * @author Programmieren-Team
 */
public enum Tokenization {

    UNDER_CAMEL(Tokenization::tokenizeUnderCamel),

    /**
     * The character CHAR tokenization.
     */
    CHAR(Tokenization::tokenizeChar),
    
    /**
     * The word WORD tokenization.
     */
    WORD(Tokenization::tokenizeWord),

    /**
     * The smart SMART tokenization.
     */
    SMART(Tokenization::tokenizeSmart);

    private final Tokenizer tokenizer;

    Tokenization(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Tokenizes a given string.
     * @param input the string to tokenize.
     * @return the tokens for that text.
     */
    public String[] tokenize(String input) {
        return this.tokenizer.tokenize(input);
    }

    private static String[] tokenizeChar(String input) {
        String[] tokens = new String[input.length()];
        for (int i = 0; i < input.length(); i++) {
            tokens[i] = String.valueOf(input.charAt(i));
        }
        return tokens;
    }

    private static String[] tokenizeWord(String input) {
        String[] tokens = input.split(" |\\r\\n|\\n");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].replaceAll("\\.|,|\\?|:|;|!|\\(|\\)", "");
        }
        return tokens;
    }
    
    private static String[] tokenizeUnderCamel(String input) {
        String[] words = input.split("(\\W|_)+");
        List<String> results = new LinkedList<>();
        for (String word : words) {
            results.addAll(splitUnderscoreCamelCase(word));
        }
        return results.toArray(new String[0]);
    }

    private static Collection<String> splitUnderscoreCamelCase(String word) {
        List<String> results = new ArrayList<>();
        int wordStart = 0;
        boolean lower = false;
        for (int i = 0; i < word.length(); i++) {
            if (Character.isUpperCase(word.charAt(i))) {
                if (lower) {
                    results.add(word.substring(wordStart, i));
                    wordStart = i;
                }
                lower = false;
            } else if (Character.isLowerCase(word.charAt(i))) {
                lower = true;
            }
        }
        results.add(word.substring(wordStart));
        return results;
    }

    private static String[] tokenizeSmart(String input) {
        List<String> tokens = new LinkedList<>();
        int wordStart = 0;
        boolean inWord = false;
        boolean specialAllowed = false;
        for (int i = 0; i < input.length(); i++) {
            if (Character.isLetterOrDigit(input.charAt(i))) {
                if (Character.isDigit(input.charAt(i))) {
                    specialAllowed = true;
                }
                if (inWord) {
                    continue;
                } else {
                    inWord = true;
                    continue;
                }
            } else {
                if (inWord) {
                    if (specialAllowed && (input.charAt(i) == '.' || input.charAt(i) == ':' || input.charAt(i) == ',')) {
                        continue;
                    } else {
                        tokens.add(input.substring(wordStart, i));
                        inWord = false;
                        specialAllowed = false;
                        wordStart = i + 1;
                    }
                } else {
                    if (specialAllowed) {
                        throw new IllegalStateException("specialAllowed unexpectedly true when !inWord && !isLetterOrDigit");
                    } else {
                        wordStart++;
                    }
                }
            }
        }
        if (inWord) {
            tokens.add(input.substring(wordStart));
        }
        return tokens.toArray(new String[0]);
    }

    @FunctionalInterface
    private interface Tokenizer {
        String[] tokenize(String input);
    }
}
