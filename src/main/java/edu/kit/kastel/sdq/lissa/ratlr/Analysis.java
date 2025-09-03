package edu.kit.kastel.sdq.lissa.ratlr;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.mcse.ardoco.metrics.result.SingleClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Analysis {

    @JsonProperty
    private final List<ElementAnalysis> sourceElements;
    @JsonProperty
    private final List<ElementAnalysis> targetElements;
    @JsonProperty
    private final List<ClassificationResultAnalysis> truePositives = new LinkedList<>();
    @JsonProperty
    private final List<ClassificationResultAnalysis> falseNegatives = new LinkedList<>();
    @JsonProperty
    private final List<ClassificationResultAnalysis> falsePositives = new LinkedList<>();
    @JsonProperty
    private final List<ClassificationResultAnalysis> trueNegatives = new LinkedList<>();

    public Analysis(List<Element> sourceElements, List<Element> targetElements, List<Pair<Element, Element>> retrieved, 
                    List<ClassificationResult> llmResults, SingleClassificationResult<TraceLink> statistics, TraceLinkIdPostprocessor postprocessor) {
        this.sourceElements = sourceElements.stream()
                .filter(element -> element.getParent() == null)
                .map(element -> new ElementAnalysis(element, sourceElements, true))
                .toList();
        this.targetElements = targetElements.stream()
                .filter(element -> element.getParent() == null)
                .map(element -> new ElementAnalysis(element, targetElements, true))
                .toList();
        for (Pair<Element, Element> retrievedPair : retrieved) {
            ClassificationResultAnalysis analysis = new ClassificationResultAnalysis(retrievedPair.first().getIdentifier(), retrievedPair.second().getIdentifier(), 
                    CacheManager.getDefaultInstance().getKey(retrievedPair.first(), retrievedPair.second()).localKey());
            TraceLink retrievedProcessed = postprocessor.postprocess(Set.of(new TraceLink(retrievedPair.first().getIdentifier(), retrievedPair.second().getIdentifier())))
                    .iterator().next();
            getConfusionCollection(statistics, retrievedProcessed).add(analysis);
        }
    }

    private List<ClassificationResultAnalysis> getConfusionCollection(SingleClassificationResult<TraceLink> statistics, TraceLink retrievedProcessed) {
        List<ClassificationResultAnalysis> collector = null;
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

    private static final class ClassificationResultAnalysis {
        @JsonProperty
        private final String sourceIdentifier;
        @JsonProperty
        private final String targetIdentifier;
        @JsonProperty
        private final String responseCacheKey;

        public ClassificationResultAnalysis(String sourceIdentifier, String targetIdentifier, String responseCacheKey) {
            this.sourceIdentifier = sourceIdentifier;
            this.targetIdentifier = targetIdentifier;
            this.responseCacheKey = responseCacheKey;
        }
    }
    
    private static final class ElementAnalysis {
        /** The unique identifier of this knowledge unit */
        @JsonProperty
        private final String identifier;
        /** The type of this knowledge unit */
        @JsonProperty
        private final String type;
        /** The original content of this knowledge unit */
        @JsonProperty
        private final String content;
        /** The granularity level of this element, indicating its level of detail */
        @JsonProperty
        private final int granularity;
        /** The parent element of this element, if any */
        @JsonProperty
        private final List<ElementAnalysis> children = new ArrayList<>();
        /**
         *  Flag indicating whether this element should be included in comparisons.
         *  {@link Classifier Classifiers} will only consider this element for candidate pairs for classification if this is true.
         */
        @JsonProperty
        private final boolean compare;

        public ElementAnalysis(Element element, List<Element> allElements, boolean root) {
            this.identifier = element.getIdentifier();
            this.type = element.getType();
            this.content = root ? "root" : element.getContent();
            this.granularity = element.getGranularity();
            this.compare = element.isCompare();
            for (Element allElement : allElements) {
                if (element.equals(allElement.getParent())) {
                    this.children.add(new ElementAnalysis(allElement, allElements, false));
                }
            }
        }
    }
}
