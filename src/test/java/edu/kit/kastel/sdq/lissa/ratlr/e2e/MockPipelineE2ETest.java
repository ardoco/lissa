/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.e2e;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import edu.kit.kastel.mcse.ardoco.llm.util.Environment;
import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;

/**
 * Runs the full LiSSA pipeline in mock mode (mock embeddings + mock classifier). This exercises the whole
 * pipeline offline, without any model access. The mock classifier links every retrieved candidate, so the
 * run must produce trace links.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockPipelineE2ETest {

    @BeforeAll
    void init() {
        Environment.overwrite(Path.of("src/test/resources/.env-test"));
    }

    @Test
    void testMockPipelineRuns() throws Exception {
        File config = new File("src/test/resources/warc/config-mock.json");
        Assertions.assertTrue(config.exists(), "mock config missing at " + config.getAbsolutePath());

        Evaluation evaluation = new Evaluation(config.toPath());
        Set<TraceLink> traceLinks = evaluation.run();

        Assertions.assertNotNull(traceLinks);
        Assertions.assertFalse(
                traceLinks.isEmpty(), "the mock classifier links every retrieved candidate, so links must exist");
    }
}
