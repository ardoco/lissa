/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.optimizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.GoldStandardConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages the storage, retrieval, and processing of classification results.
 * Handles serialization to and from JSON files, generation of example sets,
 * and comparison with gold-standard datasets.
 */
public class ClassificationResultsManager {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationResultsManager.class);

    private final Path resultsFile;
    private final ObjectMapper mapper;
    private final Random random; //used to pick random elements from a list

    /**
     * Creates a new results manager bound to the given output file.
     *
     * @param resultsFile target file for storing results
     */
    public ClassificationResultsManager(Path resultsFile) {
        this.resultsFile = resultsFile;
        this.mapper = new ObjectMapper();
        this.random = new Random(42);
    }

    /**
     * Creates a default results file path inside the given directory.
     *
     * @param baseDirectory base directory
     * @return path to default results file
     * @throws IOException if the directory cannot be created
     */
    public static Path defaultResultFile(Path baseDirectory) throws IOException {
        Files.createDirectories(baseDirectory);
        return baseDirectory.resolve("classification_results.json");
    }

    /**
     * Merges source and target elements into a single map, keyed by element identifier.
     *
     * @param sourceElements source elements
     * @param targetElements target elements
     * @return combined map of all elements
     */
    public static SortedMap<String, Element> mergeElements(Collection<Element> sourceElements, Collection<Element> targetElements) {
        SortedMap<String, Element> all = new TreeMap<>();
        for (Element e : sourceElements) all.put(e.getIdentifier(), e);
        for (Element e : targetElements) all.put(e.getIdentifier(), e);
        return all;
    }

    /**
     * Saves a list of classification results to the configured JSON file.
     *
     * @param results list of results to persist
     */
    private void saveResults(List<DetailedClassificationResult> results) {
        try {
            Files.createDirectories(resultsFile.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(resultsFile.toFile(), results);
            logger.info("Classification results written to {}", resultsFile.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write classification results file", e);
        }
    }

    /**
     * Loads classification results from the configured JSON file.
     *
     * @return loaded results, or an empty list if not found or unreadable
     */
    private List<DetailedClassificationResult> loadResults() {
        if (!Files.exists(resultsFile)) {
            logger.warn("Results file not found: {}", resultsFile);
            return List.of();
        }
        try {
            return mapper.readValue(resultsFile.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            logger.error("Error reading classification results file", e);
            return List.of();
        }
    }

    /**
     * Loads examples from the classification results and groups them
     * into true positives (TP), false positives (FP), and false negatives (FN).
     *
     * @return categorized examples
     */
    public SortedMap<String, List<String>> loadExamples() {
        SortedMap<String, List<String>> categorized = new TreeMap<>();
        categorized.put("TP", new ArrayList<>());
        categorized.put("FP", new ArrayList<>());
        categorized.put("FN", new ArrayList<>());
        for (DetailedClassificationResult detailedClassificationResult : loadResults()) {
            String category = detailedClassificationResult.getCategory().name();
            String example = String.format("[%s] Source: %s || Target: %s",
                    category, detailedClassificationResult.getSourceContent(), detailedClassificationResult.getTargetContent());
            if (categorized.containsKey(category)) {
                categorized.get(category).add(example);
            }
        }
        return categorized;
    }

    /**
     * Selects a weighted subset of examples with a fixed random seed.
     * Picks 2 FN, 2 FP, and 1 TP examples if available.
     *
     * @param examples categorized examples
     * @return randomly selected subset of examples
     */
    public List<String> pickWeightedExamples(SortedMap<String, List<String>> examples) {
        List<String> selected = new ArrayList<>();
        selected.addAll(pickRandom(examples.get("FN"), 2));
        selected.addAll(pickRandom(examples.get("FP"), 2));
        selected.addAll(pickRandom(examples.get("TP"), 1));
        return selected;
    }

    /**
     * Generates detailed classification results by comparing predicted trace links
     * against a gold-standard CSV file.
     *
     * @param traceLinks  predicted trace links
     * @param allElements all available elements
     * @param goldConfig  configuration for the gold standard CSV
     * @throws IOException if the gold CSV cannot be read
     */
    public void saveDetailedResults(Set<TraceLink> traceLinks, SortedMap<String, Element> allElements, GoldStandardConfiguration goldConfig) throws IOException {
        Set<TraceLink> goldLinks = new HashSet<>();
        boolean skipHeader = goldConfig.hasHeader();
        boolean swapColumns = goldConfig.swapColumns();

        Files.readAllLines(Path.of(goldConfig.path())).stream()
                .skip(skipHeader ? 1 : 0) // skip header if it is there
                .map(line -> line.split(","))
                .filter(parts -> parts.length == 2)
                .forEach(parts -> {
                    String source = swapColumns ? parts[1].trim() : parts[0].trim();
                    String target = swapColumns ? parts[0].trim() : parts[1].trim();
                    goldLinks.add(new TraceLink(source, target));
                });

        List<DetailedClassificationResult> results = new ArrayList<>();
        for (TraceLink link : traceLinks) {
            Element source = allElements.get(link.sourceId());
            Element target = allElements.get(link.targetId());

            DetailedClassificationResult detailedClassificationResult = new DetailedClassificationResult(
                    goldLinks.contains(link) ? DetailedClassificationResult.Category.TP : DetailedClassificationResult.Category.FP,
                    link.sourceId(),
                    source != null ? source.getContent() : "",
                    link.targetId(),
                    target != null ? target.getContent() : ""
            );

            results.add(detailedClassificationResult);
        }

        for (TraceLink link : goldLinks) {
            if (!traceLinks.contains(link)) {
                Element source = allElements.get(link.sourceId());
                Element target = allElements.get(link.targetId());

                DetailedClassificationResult cr = new DetailedClassificationResult(
                        DetailedClassificationResult.Category.FN,
                        link.sourceId(),
                        source != null ? source.getContent() : "",
                        link.targetId(),
                        target != null ? target.getContent() : ""
                );

                results.add(cr);
            }
        }
        saveResults(results);
    }


    /**
     * Picks numberOfElements random elements from a list.
     */
    private List<String> pickRandom(List<String> list, Integer numberOfElements) {
        if (list == null || list.isEmpty()) return List.of();
        List<String> copy = new ArrayList<>(list);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(numberOfElements, copy.size()));
    }
}

