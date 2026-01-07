/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// MIT License
// Adapted from LangChain (Python)
// https://github.com/langchain-ai/langchain/blob/master/libs/text-splitters/langchain_text_splitters/character.py
class RecursiveSplitter {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveSplitter.class);

    private final int chunkSize;
    private final List<String> separators;

    public RecursiveSplitter(List<String> separators, int chunkSize) {
        this.chunkSize = chunkSize;
        if (separators.isEmpty()) throw new IllegalArgumentException("No separators provided");
        this.separators = new ArrayList<>(separators);
    }

    public static RecursiveSplitter fromLanguage(Language language, int chunkSize) {
        return new RecursiveSplitter(getSeparatorsForLanguage(language), chunkSize);
    }

    public List<String> splitText(String text) {
        text = text.replace("\r\n", "\n");
        return splitText(text, separators);
    }

    private List<String> splitText(String text, List<String> separators) {

        // Get good separators .. whatever that means (see LangChain)
        List<String> finalChunks = new ArrayList<>();
        String separator = separators.getLast();
        List<String> newSeparators = new ArrayList<>();
        String _separator;
        for (int i = 0; i < separators.size(); i++) {
            _separator = separators.get(i);
            if (_separator.isEmpty()) {
                separator = "";
                break;
            }

            if (Pattern.compile(_separator).matcher(text).find()) {
                separator = _separator;
                newSeparators = separators.subList(i + 1, separators.size());
                break;
            }
        }
        _separator = separator;

        List<String> splits = splitWithRegex(text, _separator);

        // Merge splits
        List<String> goodSplits = new ArrayList<>();
        _separator = "";
        for (String s : splits) {
            if (s.length() < chunkSize) {
                goodSplits.add(s);
            } else {
                if (!goodSplits.isEmpty()) {
                    List<String> mergedText = mergeSplits(goodSplits, _separator);
                    finalChunks.addAll(mergedText);
                    goodSplits = new ArrayList<>();
                }
                if (newSeparators.isEmpty()) {
                    finalChunks.add(s);
                } else {
                    List<String> otherInfo = splitText(s, newSeparators);
                    finalChunks.addAll(otherInfo);
                }
            }
        }
        if (!goodSplits.isEmpty()) {
            List<String> mergedText = mergeSplits(goodSplits, _separator);
            finalChunks.addAll(mergedText);
        }
        return finalChunks;
    }

    private List<String> splitWithRegex(String text, @Nullable String separator) {
        if (separator == null || separator.isEmpty()) {
            return text.chars().mapToObj(it -> String.valueOf((char) it)).toList();
        }

        Pattern pattern = Pattern.compile("(" + separator + ")");
        Matcher matcher = pattern.matcher(text);
        List<String> _splits = new ArrayList<>();
        int lastEnd = 0;
        while (matcher.find()) {
            _splits.add(text.substring(lastEnd, matcher.start()));
            _splits.add(matcher.group());
            lastEnd = matcher.end();
        }
        _splits.add(text.substring(lastEnd));
        List<String> splits = new ArrayList<>();
        for (int i = 1; i < _splits.size() - 1; i += 2) {
            splits.add(_splits.get(i) + _splits.get(i + 1));
        }

        if (_splits.size() % 2 == 0) {
            splits.add(_splits.getLast());
        }

        splits.addFirst(_splits.getFirst());
        splits.removeIf(String::isEmpty);
        return splits;
    }

    private List<String> mergeSplits(List<String> splits, String separator) {
        int separatorLen = separator.length();

        List<String> docs = new ArrayList<>();

        List<String> currentDoc = new ArrayList<>();
        int total = 0;
        for (String d : splits) {
            int _len = d.length();
            if (total + _len + (!currentDoc.isEmpty() ? separatorLen : 0) > chunkSize) {
                if (total > chunkSize) {
                    logger.warn("Created a chunk of size {} which is greater than the chunk size {}", total, chunkSize);
                }
                if (!currentDoc.isEmpty()) {
                    String doc = joinDocs(currentDoc, separator);
                    if (doc != null && !doc.isBlank()) {
                        docs.add(doc);
                    }
                    while (total > 0) {
                        total -= currentDoc.getFirst().length() + (currentDoc.size() > 1 ? separatorLen : 0);
                        currentDoc.removeFirst();
                    }
                }
            }
            currentDoc.add(d);
            total += _len + (currentDoc.size() > 1 ? separatorLen : 0);
        }
        String doc = joinDocs(currentDoc, separator);
        if (doc != null && !doc.isBlank()) {
            docs.add(doc);
        }
        return docs;
    }

    private @Nullable String joinDocs(List<String> docs, String separator) {
        String text = String.join(separator, docs);
        text = text.strip();
        if (text.isBlank()) {
            return null;
        }
        return text;
    }

    public static List<String> getSeparatorsForLanguage(Language language) {
        // Taken from LangChain (Python)
        return switch (language) {
            case JAVA ->
                List.of(
                        "\nclass ",
                        "\npublic ",
                        "\nprotected ",
                        "\nprivate ",
                        "\nstatic ",
                        "\nif ",
                        "\nfor ",
                        "\nwhile ",
                        "\nswitch ",
                        "\ncase ",
                        "\n\n",
                        "\n",
                        " ",
                        "");
            case PYTHON -> List.of("\nclass ", "\ndef ", "\n\tdef ", "\n\n", "\n", " ", "");
            // Add other languages as needed...
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    public enum Language {
        JAVA,
        PYTHON
    }
}
