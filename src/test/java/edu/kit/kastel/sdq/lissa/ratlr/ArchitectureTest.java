/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;

import edu.kit.kastel.sdq.lissa.cli.command.OptimizeCommand;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizer;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Futures;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Architecture tests for the LiSSA framework using ArchUnit.
 * <p>
 * This class defines architectural rules to enforce:
 * <ul>
 *   <li>Centralized environment variable access</li>
 *   <li>Centralized UUID generation</li>
 *   <li>Functional programming practices (avoid forEach for side effects)</li>
 * </ul>
 * These rules help maintain code quality, consistency, and architectural integrity.
 */
@AnalyzeClasses(packages = "edu.kit.kastel.sdq.lissa")
class ArchitectureTest {

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
     * CacheKeys should only be created using the #of method of the CacheKey class.
     */
    @ArchTest
    static final ArchRule cacheKeysShouldBeCreatedUsingKeyGenerator = noClasses()
            .that()
            .haveNameNotMatching(CacheKey.class.getName())
            .should()
            .callConstructorWhere(new DescribedPredicate<JavaConstructorCall>("calls CacheKey constructor") {
                @Override
                public boolean test(JavaConstructorCall javaConstructorCall) {
                    return javaConstructorCall
                            .getTarget()
                            .getOwner()
                            .getFullName()
                            .equals(CacheKey.class.getName());
                }
            });

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
     * Rule that enforces that each CacheKey implementation has a static of() method.
     * <p>
     * Each class implementing CacheKey must provide a static factory method named 'of'
     * that takes a specific CacheParameter and a String as parameters. The method must
     * access all attributes of the corresponding CacheParameter.
     */
    @ArchTest
    static final ArchRule cacheKeysMustHaveOfMethodWithCacheParameter = classes()
            .that()
            .implement(CacheKey.class)
            .and()
            .areNotInterfaces()
            .should(
                    new ArchCondition<>(
                            "have a static 'of' method that takes a CacheParameter and String, and reads all CacheParameter attributes") {
                        @Override
                        public void check(JavaClass javaClass, ConditionEvents events) {
                            // Check for static 'of' method
                            var ofMethods = javaClass.getMethods().stream()
                                    .filter(m -> m.getName().equals("of"))
                                    .filter(m -> m.getModifiers().contains(JavaModifier.STATIC))
                                    .filter(m -> m.getRawParameterTypes().size() == 2)
                                    .filter(m -> m.getRawParameterTypes()
                                            .get(0)
                                            .isAssignableTo(edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter.class))
                                    .filter(m -> m.getRawParameterTypes().get(1).isAssignableTo(String.class))
                                    .toList();

                            if (ofMethods.isEmpty()) {
                                String message = String.format(
                                        "Class %s does not have a static 'of' method with signature: of(CacheParameter, String)",
                                        javaClass.getFullName());
                                events.add(violated(javaClass, message));
                                return;
                            }

                            // Check that the 'of' method reads all CacheParameter attributes
                            for (var ofMethod : ofMethods) {
                                var cacheParameterType =
                                        ofMethod.getRawParameterTypes().get(0);

                                // Get all methods of the CacheParameter (excluding inherited/generated methods)
                                var parameterMethods = cacheParameterType.getMethods().stream()
                                        .filter(m -> !m.getOwner().isEquivalentTo(Object.class))
                                        .filter(m -> !m.getName()
                                                .equals("parameters")) // parameters() is for cache naming, not
                                        // for key construction
                                        .filter(m -> !m.getName().equals("equals")) // Auto-generated by record
                                        .filter(m -> !m.getName().equals("hashCode")) // Auto-generated by record
                                        .filter(m -> !m.getName().equals("toString")) // Auto-generated by record
                                        .toList();

                                // Get all method calls in the 'of' method
                                var methodCallsInOf = ofMethod.getMethodCallsFromSelf();
                                Set<String> calledMethodNames = methodCallsInOf.stream()
                                        .map(call -> call.getTarget().getName())
                                        .collect(Collectors.toSet());

                                // Check if all parameter methods are called
                                for (var paramMethod : parameterMethods) {
                                    boolean isCalled = calledMethodNames.contains(paramMethod.getName());

                                    if (!isCalled) {
                                        String message = String.format(
                                                "Method %s.of() does not read CacheParameter attribute '%s'",
                                                javaClass.getSimpleName(), paramMethod.getName());
                                        events.add(violated(javaClass, message));
                                    }
                                }
                            }
                        }
                    });

    /**
     * Rule that enforces that the parameters() method in each CacheParameter implementation accesses all fields.
     * <p>
     * Each class implementing CacheParameter must have a parameters() method that uses all record components/fields
     * to ensure the cache key is unique and complete.
     */
    @ArchTest
    static final ArchRule cacheParametersMustUseAllFieldsInParametersMethod = classes()
            .that()
            .implement(edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter.class)
            .and()
            .areNotInterfaces()
            .should(new ArchCondition<>("have a parameters() method that accesses all fields") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    // Find the parameters() method
                    var parametersMethod = javaClass.getMethods().stream()
                            .filter(m -> m.getName().equals("parameters"))
                            .filter(m -> m.getRawParameterTypes().isEmpty())
                            .findFirst();

                    if (parametersMethod.isEmpty()) {
                        String message =
                                String.format("Class %s does not have a parameters() method", javaClass.getFullName());
                        events.add(violated(javaClass, message));
                        return;
                    }

                    // Get all fields of the CacheParameter (record components)
                    var fields = javaClass.getAllFields().stream()
                            .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
                            .toList();

                    if (fields.isEmpty()) {
                        return; // No fields to check
                    }

                    var method = parametersMethod.get();

                    // Get all field accesses in the parameters() method
                    var fieldAccesses = method.getFieldAccesses();
                    Set<String> accessedFieldNames = fieldAccesses.stream()
                            .map(access -> access.getTarget().getName())
                            .collect(Collectors.toSet());

                    // Also check for method calls (record accessor methods)
                    var methodCalls = method.getMethodCallsFromSelf();
                    Set<String> calledMethodNames = methodCalls.stream()
                            .map(call -> call.getTarget().getName())
                            .collect(Collectors.toSet());

                    // Check if all fields are accessed (either directly or via accessor methods)
                    for (var field : fields) {
                        String fieldName = field.getName();
                        boolean isAccessed =
                                accessedFieldNames.contains(fieldName) || calledMethodNames.contains(fieldName);

                        if (!isAccessed) {
                            String message = String.format(
                                    "Method %s.parameters() does not access field '%s'",
                                    javaClass.getSimpleName(), fieldName);
                            events.add(violated(javaClass, message));
                        }
                    }
                }
            });
}
