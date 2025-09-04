package edu.kit.kastel.sdq.lissa.ratlr;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Analysis {

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
                    List<ClassificationResult> llmResults, SingleClassificationResult<TraceLink> statistics, TraceLinkIdPostprocessor postprocessor) {
        this.sourceElements = sourceElements.stream()
                .filter(element -> element.getParent() == null)
                .map(element -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(element.getIdentifier(), getElementMap(element, sourceElements, true));
                    return map;
                })
                .toList();
        this.targetElements = targetElements.stream()
                .filter(element -> element.getParent() == null)
                .map(element -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put(element.getIdentifier(), getElementMap(element, targetElements, true));
                    return map;
                })
                .toList();
        for (Pair<Element, Element> retrievedPair : retrieved) {
            CacheKey key = CacheManager.getDefaultInstance().getKey(retrievedPair.first(), retrievedPair.second());
            TraceLink retrievedProcessed = postprocessor.postprocess(Set.of(new TraceLink(retrievedPair.first().getIdentifier(), retrievedPair.second().getIdentifier())))
                    .iterator().next();
            Map<String, Map<String, String>> confusionCollection = getConfusionCollection(statistics, retrievedProcessed);
            confusionCollection.putIfAbsent(retrievedPair.first().getIdentifier(), new LinkedHashMap<>());
            confusionCollection.get(retrievedPair.first().getIdentifier()).put(retrievedPair.second().getIdentifier(), key == null ? null : key.localKey());
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
}
