/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import java.util.*;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * A preprocessor that creates a hierarchical tree structure from code artifacts,
 * organizing them by package and class. This preprocessor considers both the
 * code files and their package declarations to create a meaningful hierarchy.
 *
 * The preprocessor creates a tree structure where:
 * <ul>
 *     <li>Root elements represent packages (granularity level 0)</li>
 *     <li>Child elements represent classes within packages (granularity level 1)</li>
 * </ul>
 *
 * For each package, the preprocessor:
 * <ul>
 *     <li>Creates a package element with a description of its contents</li>
 *     <li>Groups all classes that belong to that package</li>
 *     <li>Creates class elements as children of the package element</li>
 * </ul>
 *
 * Configuration options:
 * <ul>
 *     <li>language: The programming language to use (currently only JAVA is supported)</li>
 *     <li>compare_classes: Whether class elements should be marked for comparison (default: false)</li>
 * </ul>
 *
 * Each element in the hierarchy:
 * <ul>
 *     <li>Has a unique identifier (package name or class identifier)</li>
 *     <li>Has a specific type ("source code package definition" or "source code class definition")</li>
 *     <li>Contains either a package description or the full class content</li>
 *     <li>Has a granularity level based on its position in the hierarchy</li>
 *     <li>Is marked for comparison based on its type and configuration</li>
 * </ul>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class CodeTreePreprocessor extends Preprocessor<Artifact> {

    /** The programming language to use for parsing */
    private final Language language;
    /** Whether class elements should be marked for comparison */
    private final boolean compareClasses;

    /**
     * Creates a new code tree preprocessor with the specified configuration and context store.
     *
     * @param configuration The module configuration containing language and comparison settings
     * @param contextStore The shared context store for pipeline components
     */
    public CodeTreePreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.language = Language.valueOf(configuration.argumentAsString("language", "JAVA"));
        this.compareClasses = configuration.argumentAsBoolean("compare_classes", false);
    }

    /**
     * Preprocesses a list of code artifacts by organizing them into a package-class hierarchy.
     * The specific organization strategy is determined by the configured language.
     *
     * @param artifacts The list of code artifacts to preprocess
     * @return A list of elements representing the package-class hierarchy
     */
    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        return switch (language) {
            case JAVA -> createJavaTree(artifacts);
        };
    }

    /**
     * Creates a hierarchical tree structure from Java code artifacts.
     * This method:
     * <ol>
     *     <li>Groups artifacts by their package declarations</li>
     *     <li>Creates package elements with descriptions of their contents</li>
     *     <li>Creates class elements as children of their respective packages</li>
     * </ol>
     *
     * The method handles both artifacts with and without package declarations,
     * placing those without package declarations in the default (empty) package.
     *
     * @param artifacts The list of Java code artifacts to organize
     * @return A list of elements representing the package-class hierarchy
     */
    private List<Element> createJavaTree(List<Artifact> artifacts) {
        List<Element> result = new ArrayList<>();

        Map<String, List<Artifact>> packagesToClasses = new HashMap<>();
        for (Artifact artifact : artifacts) {
            List<String> packageDeclaration = Arrays.stream(
                            artifact.getContent().split("\n"))
                    .filter(line -> line.trim().startsWith("package"))
                    .toList();
            assert packageDeclaration.size() <= 1;
            String packageName = packageDeclaration.isEmpty()
                    ? ""
                    : packageDeclaration.getFirst().split(" ")[1].replace(";", "");
            packagesToClasses.putIfAbsent(packageName, new ArrayList<>());
            packagesToClasses.get(packageName).add(artifact);
        }

        for (Map.Entry<String, List<Artifact>> entry : packagesToClasses.entrySet()) {
            String packageName = entry.getKey();
            List<Artifact> classes = entry.getValue();
            String packageDescription =
                    """
                    This package is called %s and contains the following classes: %s
                    """
                            .formatted(
                                    packageName,
                                    classes.stream()
                                            .map(Artifact::getIdentifier)
                                            .toList());

            Element packageElement = new Element(
                    "package-" + packageName, "source code package definition", packageDescription, 0, null, true);
            result.add(packageElement);
            for (Artifact clazz : classes) {
                Element classElement = new Element(
                        clazz.getIdentifier(),
                        "source code class definition",
                        clazz.getContent(),
                        1,
                        packageElement,
                        compareClasses);
                result.add(classElement);
            }
        }
        return result;
    }

    /**
     * Enum representing supported programming languages for tree creation.
     * Currently, only Java is supported.
     */
    public enum Language {
        /** Java programming language */
        JAVA
    }
}
