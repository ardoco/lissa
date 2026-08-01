/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import edu.kit.kastel.mcse.ardoco.llm.cache.CacheManager;
import edu.kit.kastel.mcse.ardoco.llm.util.Environment;
import edu.kit.kastel.mcse.ardoco.llm.util.Futures;
import edu.kit.kastel.mcse.ardoco.llm.util.KeyGenerator;
import edu.kit.kastel.sdq.lissa.cli.command.OptimizeCommand;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizer;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;

/**
 * Architecture tests for the LiSSA framework using ArchUnit.
 * <p>
 * These rules help maintain code quality, consistency, and architectural integrity. Rules that used to
 * enforce invariants of the LLM/cache utilities now live in the {@code llm-access} library, which owns
 * those classes.
 */
@AnalyzeClasses(packages = "edu.kit.kastel.sdq.lissa")
class ArchitectureTest {

    @ArchTest
    static final ArchRule allLoggerShallBeCalledLogger = fields().that()
            .haveRawType(Logger.class)
            .should()
            .haveName("logger")
            .orShould()
            .haveName("STATIC_LOGGER") // Exception for cases where multiple loggers are needed
            .because("All loggers should be named 'logger' for consistency.");

    /**
     * Rule that enforces environment variable access restrictions.
     * <p>
     * Only the {@link Environment} utility class may call {@code System.getenv()}.
     * All other classes must use the {@link Environment} class for environment variable access.
     */
    @ArchTest
    static final ArchRule noDirectEnvironmentAccess = noClasses()
            .that()
            .haveNameNotMatching(Environment.class.getName())
            .and()
            .resideOutsideOfPackage("..e2e..")
            .should()
            .callMethod(System.class, "getenv")
            .orShould()
            .callMethod(System.class, "getenv", String.class);

    /**
     * Rule that enforces UUID generation restrictions.
     * <p>
     * Only the {@link KeyGenerator} utility class may access {@link UUID}.
     * All other classes must use the {@link KeyGenerator} for UUID generation.
     */
    @ArchTest
    static final ArchRule onlyKeyGeneratorAllowedForUUID = noClasses()
            .that()
            .haveNameNotMatching(KeyGenerator.class.getName())
            .should()
            .accessClassesThat()
            .haveNameMatching(UUID.class.getName());

    /**
     * Rule that enforces functional programming practices.
     * <p>
     * Discourages the use of {@code forEach} and {@code forEachOrdered} on streams and lists,
     * as these are typically used for side effects. Prefer functional operations instead.
     */
    @ArchTest
    static final ArchRule noForEachInCollectionsOrStream = noClasses()
            .should()
            .callMethod(Stream.class, "forEach", Consumer.class)
            .orShould()
            .callMethod(Stream.class, "forEachOrdered", Consumer.class)
            .orShould()
            .callMethod(List.class, "forEach", Consumer.class)
            .orShould()
            .callMethod(List.class, "forEachOrdered", Consumer.class)
            .because("Lambdas should be functional. ForEach is typically used for side-effects.");

    /**
     * Prompts for classifiers should only be modified by optimizers or metric scorers. Otherwise, there will be
     * inconsistencies with the configuration file.
     */
    @ArchTest
    static final ArchRule classifierPromptsShouldOnlyBeModifiedByOptimizers = noClasses()
            .that()
            .areNotAssignableTo(PromptOptimizer.class)
            .and()
            .areNotAssignableTo(Metric.class)
            .should()
            .callMethod(Classifier.class, "setClassificationPrompt", String.class);

    /**
     * Only the {@link OptimizeCommand} should be allowed to overwrite the prompt used for evaluation to reflect the
     * modified prompt into the configuration.
     */
    @ArchTest
    static final ArchRule onlyOptimizationCommandShouldCallEvaluationWithPromptOverwrite = noClasses()
            .that()
            .areNotAssignableTo(OptimizeCommand.class)
            .should()
            .callConstructor(Evaluation.class, Path.class, String.class);

    /**
     * Futures should be opened with a logger.
     */
    @ArchTest
    static final ArchRule futuresShouldBeOpenedWithLogger = noClasses()
            .that()
            .doNotHaveFullyQualifiedName(Futures.class.getName())
            .should()
            .callMethod(Future.class, "get")
            .orShould()
            .callMethod(Future.class, "resultNow");

    /**
     * Rule that enforces that CacheManager.resetDefaultInstance() is only called from Test classes.
     * <p>
     * The resetDefaultInstance() method should only be used to reset the singleton state between tests.
     */
    @ArchTest
    static final ArchRule cacheManagerResetOnlyInTests = noClasses()
            .that()
            .haveNameNotMatching(".*Test.*")
            .should()
            .callMethod(CacheManager.class, "resetDefaultInstance")
            .because(
                    "CacheManager.resetDefaultInstance() is only intended for testing purposes and must not be used elsewhere");

    /**
     * Rule that enforces that Environment.overwrite() is only called from test classes.
     * <p>
     * The overwrite() method is intended for testing purposes to override environment variables.
     * For production usage the regular .env file shall be used.
     */
    @ArchTest
    static final ArchRule environmentOverwriteOnlyInTests = noClasses()
            .that()
            .haveNameNotMatching(".*Test.*")
            .should()
            .callMethod(Environment.class, "overwrite", Path.class)
            .because(
                    "Environment.overwrite() is only intended for testing purposes and may not be used elsewhere. Use the regular .env instead.");
}
