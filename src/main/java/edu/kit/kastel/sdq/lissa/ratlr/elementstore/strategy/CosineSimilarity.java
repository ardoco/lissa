/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * A retrieval strategy that computes the cosine similarity between a query vector and
 * vectors of elements in the store. This strategy is used for finding similar elements
 * based on their vector embeddings.
 * It supports a configurable maximum number of results to return.
 */
public class CosineSimilarity extends MaxResultsStrategy {

    public CosineSimilarity(ModuleConfiguration configuration) {
        super(configuration);
    }

    /**
     * Creates a new instance limiting the returned results.
     * @param maxResults the number of the highest results to be returned
     */
    CosineSimilarity(int maxResults) {
        super(maxResults);
    }

    @Override
    public List<Pair<Element, Float>> findSimilarElementsInternal(
            Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        List<Pair<Element, Float>> similarElements = new ArrayList<>();
        for (var element : allElementsInStore) {
            float[] elementVector = element.second();
            float similarity = cosineSimilarity(query.second(), elementVector);
            similarElements.add(new Pair<>(element.first(), similarity));
        }
        return similarElements;
    }

    private float cosineSimilarity(float[] queryVector, float[] elementVector) {
        if (queryVector.length != elementVector.length) {
            throw new IllegalArgumentException("The length of the query vector and the element vector must be equal.");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < queryVector.length; i++) {
            dotProduct += queryVector[i] * elementVector[i];
            normA += Math.pow(queryVector[i], 2);
            normB += Math.pow(elementVector[i], 2);
        }
        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
