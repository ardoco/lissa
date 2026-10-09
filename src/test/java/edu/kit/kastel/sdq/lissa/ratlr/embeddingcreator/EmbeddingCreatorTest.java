/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Tests the mock embedding configuration: it needs no {@code model} argument and produces zero vectors.
 */
class EmbeddingCreatorTest {

    private Element element(String id, String content) {
        return new Element(id, "requirement", content, 0, null, true);
    }

    @Test
    void mockEmbeddingConfigurationNeedsNoModelAndReturnsZeroVectors() {
        // No "model" argument: the mock creator ignores it, so none is required (and a stray one would be
        // reported as an unread parameter by ModuleConfiguration).
        ModuleConfiguration configuration = new ModuleConfiguration("mock", Map.of());
        EmbeddingCreator creator = EmbeddingCreator.createEmbeddingCreator(configuration, new ContextStore());

        Assertions.assertArrayEquals(
                new float[] {0}, creator.calculateEmbedding(element("R1", "the system shall log in")));

        List<float[]> embeddings = creator.calculateEmbeddings(List.of(element("R1", "a"), element("R2", "b")));
        Assertions.assertEquals(2, embeddings.size());
        Assertions.assertArrayEquals(new float[] {0}, embeddings.get(0));
        Assertions.assertArrayEquals(new float[] {0}, embeddings.get(1));
    }
}
