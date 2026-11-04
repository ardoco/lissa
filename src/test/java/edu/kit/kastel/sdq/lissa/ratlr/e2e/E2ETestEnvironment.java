/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.e2e;

import java.util.Map;

import edu.kit.kastel.mcse.ardoco.llm.util.EnvironmentProvider;
import edu.kit.kastel.mcse.ardoco.llm.util.MapEnvironment;

/**
 * The environment of the end-to-end tests. They replay the committed caches and must never access a real model.
 * <p>
 * The credentials and hosts are dummies that only satisfy the configuration checks of the model platforms, so a
 * cache miss fails instead of querying a real model. As a {@link MapEnvironment} has no fallback to the process
 * environment, real keys exported in the shell cannot leak into the tests. The caches are local only
 * ({@code CACHE_HIERARCHY=LOCAL}), and inconsistencies between cache layers fail
 * ({@code CACHE_REPLACEMENT_STRATEGY=ERROR}).
 */
final class E2ETestEnvironment {

    static final EnvironmentProvider ENVIRONMENT = new MapEnvironment(Map.of(
            "CACHE_HIERARCHY", "LOCAL",
            "CACHE_REPLACEMENT_STRATEGY", "ERROR",
            "OPENAI_ORGANIZATION_ID", "DUMMY",
            "OPENAI_API_KEY", "DUMMY",
            "OLLAMA_HOST", "http://localhost:11434",
            "OLLAMA_EMBEDDING_HOST", "http://localhost:11434"));

    private E2ETestEnvironment() {
        throw new IllegalAccessError("Utility class");
    }
}
