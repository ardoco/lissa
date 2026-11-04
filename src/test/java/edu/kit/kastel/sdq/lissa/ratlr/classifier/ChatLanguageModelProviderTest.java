/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.kit.kastel.mcse.ardoco.llm.util.MapEnvironment;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

/**
 * Tests the platform handling of {@link ChatLanguageModelProvider}.
 */
class ChatLanguageModelProviderTest {

    @Test
    void bifrostUsesOneHundredThreads() {
        ModuleConfiguration configuration = new ModuleConfiguration("simple_bifrost", Map.of());
        Assertions.assertEquals(100, ChatLanguageModelProvider.threads(configuration));
    }

    @ParameterizedTest
    @ValueSource(strings = {"simple_deepseek", "reasoning_deepseek"})
    void deepSeekIsRejectedForThreads(String name) {
        ModuleConfiguration configuration = new ModuleConfiguration(name, Map.of());
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChatLanguageModelProvider.threads(configuration));
    }

    @Test
    void deepSeekIsRejectedForProviders() {
        ModuleConfiguration configuration =
                new ModuleConfiguration("simple_deepseek", Map.of("model", "deepseek-chat"));
        MapEnvironment environment = new MapEnvironment(Map.of("DEEPSEEK_API_KEY", "DUMMY"));
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> new ChatLanguageModelProvider(configuration, environment));
    }
}
