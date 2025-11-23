package edu.kit.kastel.sdq.lissa.ratlr;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Analysis {

    @JsonProperty
    private final float snF1;
    @JsonProperty
    private final float snRecall;
    @JsonProperty
    private final float snPrecision;
    @JsonProperty
    private final float snRecallClean;
    @JsonProperty
    private final float snPrecisionClean;
    @JsonProperty
    private final Map<Integer, String> sentenceF1 = new LinkedHashMap<>();
    @JsonProperty
    private final Map<Integer, String> sentenceRecall = new LinkedHashMap<>();
    @JsonProperty
    private final Map<Integer, String> sentencePrecision = new LinkedHashMap<>();
    @JsonProperty
    private final Map<Integer, SentenceAnalysis> statisticsBySentenceId = new LinkedHashMap<>();
    @JsonProperty
    private final List<Map<String, Object>> sourceElements;
    @JsonProperty
    private final List<Map<String, Object>> targetElements;
    @JsonProperty
    private final Map<String, Map<String, String>> truePositives = new LinkedHashMap<>();
    @JsonProperty
    private final Map<String, Map<String, String>> falseNegatives = new LinkedHashMap<>();
    @JsonProperty
    private final Map<String, Map<String, String>> falsePositives = new LinkedHashMap<>();
    @JsonProperty
    private final Map<String, Map<String, String>> trueNegatives = new LinkedHashMap<>();

    public Analysis(List<Element> sourceElements, List<Element> targetElements, List<Pair<Element, Element>> retrieved,
                    List<ClassificationResult> llmResults, ResultAggregator aggregator, TraceLinkIdPostprocessor postprocessor, 
                    SingleClassificationResult<TraceLink> statistics) {

        int max = postprocessor.postprocess(aggregator.aggregate(sourceElements, targetElements,
                        sourceElements.stream()
                                .map(source -> new ClassificationResult(source, targetElements.getFirst(), 1.0)).toList()))
                .stream()
                .map(TraceLink::sourceId)
                .map(Integer::parseInt)
                .max(Integer::compare)
                .orElseThrow();
        Map<Integer, Integer> truePositivesBySentenceId = new LinkedHashMap<>();
        collectSentenceStatistics(statistics.getTruePositives(), truePositivesBySentenceId, false);
        Map<Integer, Integer> falseNegativesBySentenceId = new LinkedHashMap<>();
        collectSentenceStatistics(statistics.getFalseNegatives(), falseNegativesBySentenceId, true);
        Map<Integer, Integer> falsePositivesBySentenceId = new LinkedHashMap<>();
        collectSentenceStatistics(statistics.getFalsePositives(), falsePositivesBySentenceId, false);
        for (int i = 1; i <= max; i++) {
            statisticsBySentenceId.put(i, new SentenceAnalysis(
                    truePositivesBySentenceId.getOrDefault(i, 0), 
                    falsePositivesBySentenceId.getOrDefault(i, 0), 
                    falseNegativesBySentenceId.getOrDefault(i, 0)));
            SentenceAnalysis sentenceAnalysis = statisticsBySentenceId.get(i);
            sentencePrecision.put(i, getHistogramValue(sentenceAnalysis.precision, max, i) + "| " + sentenceAnalysis.predictedPos);
            sentenceRecall.put(i, getHistogramValue(sentenceAnalysis.recall, max, i) + "| " + sentenceAnalysis.expectedPos);
            sentenceF1.put(i, getHistogramValue(sentenceAnalysis.f1, max, i));
        }
        float sumF1 = 0;
        float sumRecall = 0;
        float sumPrecision = 0;
        float sumRecallClean = 0;
        int recallCleanCount = 0;
        float sumPrecisionClean = 0;
        int precisionCleanCount = 0;
        for (SentenceAnalysis analysis : statisticsBySentenceId.values()) {
            sumF1 += analysis.f1;
            sumRecall += analysis.recall;
            sumPrecision += analysis.precision;
            
            if (analysis.expectedPos != 0) {
                recallCleanCount++;
                sumRecallClean += analysis.recall;
            }
            if (analysis.predictedPos != 0) {
                precisionCleanCount++;
                sumPrecisionClean += analysis.precision;
            }
        }
        snF1 = sumF1 / (float) statisticsBySentenceId.size();
        snRecall = sumRecall / (float) statisticsBySentenceId.size();
        snPrecision = sumPrecision / (float) statisticsBySentenceId.size();
        snRecallClean = sumRecallClean / (float) recallCleanCount;
        snPrecisionClean = sumPrecisionClean / (float) precisionCleanCount;
        
        this.sourceElements = sourceElements.stream()
                .filter(element -> element.getParent() == null)
                .map(element -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(element.getIdentifier(), getElementMap(element, sourceElements, false));
                    return map;
                })
                .toList();
        this.targetElements = targetElements.stream()
                .filter(element -> element.getParent() == null)
                .map(element -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(element.getIdentifier(), getElementMap(element, targetElements, false));
                    return map;
                })
                .toList();

        for (Pair<Element, Element> retrievedPair : retrieved) {
            CacheKey key = CacheManager.getDefaultInstance().getKey(retrievedPair.first(), retrievedPair.second());
            TraceLink retrievedProcessed = postprocessor.postprocess(aggregator.aggregate(sourceElements, targetElements, 
                            List.of(new ClassificationResult(retrievedPair.first(), retrievedPair.second(), 1.0))))
                    .iterator().next();
            Map<String, Map<String, String>> confusionCollection = getConfusionCollection(statistics, retrievedProcessed);
            confusionCollection.putIfAbsent(retrievedPair.first().getIdentifier(), new LinkedHashMap<>());
            confusionCollection.get(retrievedPair.first().getIdentifier()).put(retrievedPair.second().getIdentifier(), key == null ? null : key.localKey());
        }
    }

    @NotNull
    private static String getHistogramValue(float value, int maxId, int id) {
        int shifted = (int) (value * 100);
        return " ".repeat(String.valueOf(maxId).length() - String.valueOf(id).length()) + "|".repeat(shifted) + " ".repeat(100 - shifted);
    }

    private static void collectSentenceStatistics(Set<TraceLink> traceLinks, Map<Integer, Integer> collector, boolean print) {
        for (TraceLink traceLink : traceLinks) {
            int sourceId = Integer.parseInt(traceLink.sourceId());
            collector.putIfAbsent(sourceId, 0);
            collector.computeIfPresent(sourceId, (key, value) -> ++value);
        }
    }

    private Map<String, Map<String, String>> getConfusionCollection(SingleClassificationResult<TraceLink> statistics, TraceLink retrievedProcessed) {
        Map<String, Map<String, String>> collector = null;
        for (TraceLink truePositive : statistics.getTruePositives()) {
            if (retrievedProcessed.sourceId().equals(truePositive.sourceId()) && retrievedProcessed.targetId().equals(truePositive.targetId())) {
                collector = this.truePositives;
                break;
            }
        }
        if (collector == null) {
            for (TraceLink falsePositive : statistics.getFalsePositives()) {
                if (retrievedProcessed.sourceId().equals(falsePositive.sourceId()) && retrievedProcessed.targetId().equals(falsePositive.targetId())) {
                    collector = this.falsePositives;
                    break;
                }
            }
        }
        if (collector == null) {
            for (TraceLink falseNegative : statistics.getFalseNegatives()) {
                if (retrievedProcessed.sourceId().equals(falseNegative.sourceId()) && retrievedProcessed.targetId().equals(falseNegative.targetId())) {
                    collector = this.falseNegatives;
                    break;
                }
            }
        }
        if (collector == null) {
            collector = this.trueNegatives;
        }
        return collector;
    }

    private static Map<String, Object> getElementMap(Element element, List<Element> allElements, boolean root) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", element.getType());
        map.put("content", root ? "root" : element.getContent());
        map.put("granularity", element.getGranularity());
        map.put("compare", element.isCompare());
        for (Element allElement : allElements) {
            if (element.equals(allElement.getParent())) {
                map.put(allElement.getIdentifier(), getElementMap(allElement, allElements, false));
            }
        }
        return map;
    }
    
    private static final class SentenceAnalysis {
        @JsonProperty
        private final int truePositives;
        @JsonProperty
        private final int falsePositives;
        @JsonProperty
        private final int falseNegatives;
        @JsonProperty
        private final float precision;
        @JsonProperty
        private final float recall;
        @JsonProperty
        private final float f1;
        @JsonProperty
        private final int predictedPos;
        @JsonProperty
        private final int expectedPos;

        private SentenceAnalysis(int truePositives, int falsePositives, int falseNegatives) {
            this.truePositives = truePositives;
            this.falsePositives = falsePositives;
            this.falseNegatives = falseNegatives;
            this.predictedPos = truePositives + falsePositives;
            this.precision = predictedPos == 0 ? 1.0f : ((float) truePositives) / predictedPos;
            this.expectedPos = truePositives + falseNegatives;
            this.recall = expectedPos == 0 ? 1.0f : ((float) truePositives) / expectedPos;
            this.f1 = (truePositives + falsePositives + falseNegatives) == 0 ? 1.0f : (2 * truePositives) / (2.0f *truePositives + falsePositives + falseNegatives);
        }
    }
}
