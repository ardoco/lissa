/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import edu.kit.kastel.mcse.ardoco.llm.cache.CacheKey;
import edu.kit.kastel.mcse.ardoco.llm.cache.CacheParameter;
import edu.kit.kastel.mcse.ardoco.llm.chat.LazyChatModel;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Architecture tests for the {@code llm-access} library that LiSSA depends on.
 * <p>
 * The cache-key, cache-parameter and chat-model utilities that used to live in LiSSA now reside in the
 * {@code llm-access} library. The structural invariants that guaranteed their correctness (complete cache
 * keys, complete cache-file names, fully overridden lazy chat model) are just as important for LiSSA, which
 * relies on those classes for caching correctness. These invariants are no longer covered by
 * {@link ArchitectureTest}, so this class re-introduces the rules.
 * <p>
 * The analysis covers both the library package (available on the test classpath as a dependency) and the
 * LiSSA packages, so that LiSSA's own usage of these classes is checked as well (for example, LiSSA must not
 * bypass the cache-key factory methods by constructing key implementations directly).
 */
@AnalyzeClasses(packages = {"edu.kit.kastel.mcse.ardoco.llm", "edu.kit.kastel.sdq.lissa"})
class LlmAccessArchitectureTest {

    /**
     * Rule that enforces that {@link CacheKey} implementations are only created via their static factory
     * methods.
     * <p>
     * External code should not directly instantiate {@link CacheKey} implementations. Instead, they should
     * use the static factory methods (typically {@code of()}) provided by each implementation or let the
     * {@link CacheParameter#createCacheKey(String)} method handle key creation.
     * <p>
     * This rule checks that constructors of classes implementing {@link CacheKey} are not called from
     * outside those classes themselves (constructors are private and only called from static factory
     * methods).
     */
    @ArchTest
    static final ArchRule cacheKeysShouldBeCreatedUsingKeyGenerator = noClasses()
            .that()
            .doNotImplement(CacheKey.class) // Exclude CacheKey implementations themselves
            .should()
            .callConstructorWhere(
                    new DescribedPredicate<JavaConstructorCall>("calls CacheKey implementation constructor") {
                        @Override
                        public boolean test(JavaConstructorCall javaConstructorCall) {
                            return javaConstructorCall.getTarget().getOwner().isAssignableTo(CacheKey.class);
                        }
                    });

    /**
     * Rule that enforces that each {@link CacheKey} implementation has a static {@code of()} method.
     * <p>
     * Each class implementing {@link CacheKey} must provide a static factory method named {@code of}
     * that takes a specific {@link CacheParameter} and a String as parameters. The method must
     * access all record components (accessor methods) of the corresponding {@link CacheParameter}.
     * <p>
     * This ensures that all configuration parameters (model name, seed, temperature, etc.)
     * are properly used when creating cache keys, making the cache keys complete and unique.
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
                                    .filter(m -> m.getRawParameterTypes().get(0).isAssignableTo(CacheParameter.class))
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

                                // Get all accessor methods of the CacheParameter record components
                                // Exclude inherited methods, utility methods, and factory methods
                                var parameterMethods = cacheParameterType.getMethods().stream()
                                        .filter(m -> !m.getOwner().isEquivalentTo(Object.class))
                                        // parameters() generates cache file name, not used in key creation
                                        .filter(m -> !m.getName().equals("parameters"))
                                        // createCacheKey() is the factory method called by Cache, not by of()
                                        .filter(m -> !m.getName().equals("createCacheKey"))
                                        // Default methods from Object
                                        .filter(m -> !m.getName().equals("equals"))
                                        .filter(m -> !m.getName().equals("hashCode"))
                                        .filter(m -> !m.getName().equals("toString"))
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
     * Rule that enforces that the {@code parameters()} method in each {@link CacheParameter} implementation
     * accesses all fields.
     * <p>
     * Each class implementing {@link CacheParameter} must have a {@code parameters()} method that uses all
     * record components/fields to ensure the cache key is unique and complete.
     */
    @ArchTest
    static final ArchRule cacheParametersMustUseAllFieldsInParametersMethod = classes()
            .that()
            .implement(CacheParameter.class)
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

    /**
     * Rule that enforces that {@link LazyChatModel} must override all methods declared in {@link ChatModel},
     * including default methods.
     * <p>
     * This ensures that {@link LazyChatModel} provides its own implementation for all {@link ChatModel}
     * methods, preventing accidental usage of default implementations that may not be suitable for the lazy
     * loading behavior of {@link LazyChatModel}.
     */
    @ArchTest
    static final ArchRule lazyChatModelMustOverrideAllChatModelMethods = classes()
            .that()
            .haveFullyQualifiedName(LazyChatModel.class.getName())
            .should()
            .implement(ChatModel.class)
            .andShould(new ArchCondition<>("override all methods declared in ChatModel (including default methods)") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    JavaClass chatModel = clazz.getRawInterfaces().stream()
                            .filter(i -> i.getName().equals(ChatModel.class.getName()))
                            .findFirst()
                            .orElseThrow();

                    for (JavaMethod interfaceMethod : chatModel.getMethods()) {
                        boolean overridden = clazz.getMethods().stream()
                                .filter(m -> m.getOwner().equals(clazz)) // only methods declared in this class
                                .anyMatch(m -> m.getName().equals(interfaceMethod.getName())
                                        && m.getRawParameterTypes().equals(interfaceMethod.getRawParameterTypes()));

                        if (!overridden) {
                            events.add(SimpleConditionEvent.violated(
                                    clazz, "Does not override method: " + interfaceMethod.getFullName()));
                        }
                    }
                }
            });
}
