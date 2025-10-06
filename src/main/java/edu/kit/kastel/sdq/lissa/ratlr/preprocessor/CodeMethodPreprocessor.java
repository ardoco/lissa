/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * A preprocessor that splits source code artifacts into class and method elements.
 * This preprocessor creates a hierarchical structure of elements where:
 * <ul>
 *     <li>The root element represents the entire code file (granularity level 0)</li>
 *     <li>Child elements represent class definitions (granularity level 1)</li>
 *     <li>Grandchild elements represent methods within classes (granularity level 2)</li>
 * </ul>
 *
 * The preprocessor uses Tree-sitter to parse the source code and extract class and
 * method definitions. Currently, it supports Java code through the TreeSitterJava parser.
 * <p>
 * Configuration options:
 * <ul>
 *     <li>language: The programming language to use (currently only JAVA is supported)</li>
 * </ul>
 *
 * Each element in the hierarchy:
 * <ul>
 *     <li>Has a unique identifier combining its parent's ID and its index</li>
 *     <li>Has a specific type ("source code class definition" or "source code method")</li>
 *     <li>Contains the relevant portion of the source code as its content</li>
 *     <li>Has a granularity level based on its position in the hierarchy</li>
 *     <li>Is marked for comparison only if it's a method element</li>
 * </ul>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class CodeMethodPreprocessor extends Preprocessor {

    /** The programming language to use for parsing */
    private final Language language;

    /**
     * Creates a new code method preprocessor with the specified configuration and context store.
     *
     * @param configuration The module configuration containing the language setting
     * @param contextStore The shared context store for pipeline components
     */
    public CodeMethodPreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.language = Language.valueOf(configuration.argumentAsString("language", "JAVA"));
    }

    /**
     * Preprocesses a list of code artifacts by splitting each one into class and method elements.
     * For each artifact, this method:
     * <ol>
     *     <li>Creates an element representing the entire code file</li>
     *     <li>Parses the code to identify classes and methods</li>
     *     <li>Creates elements for each class and method</li>
     *     <li>Links elements in a hierarchical structure</li>
     * </ol>
     *
     * @param artifacts The list of code artifacts to preprocess
     * @return A list of elements containing the original code files and their classes and methods
     */
    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        List<Element> elements = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            List<Element> preprocessed = preprocess(artifact);
            elements.addAll(preprocessed);
        }
        return elements;
    }

    /**
     * Preprocesses a single code artifact by splitting it into class and method elements.
     * This method:
     * <ol>
     *     <li>Creates an element for the entire code file (granularity level 0)</li>
     *     <li>Parses the code using the appropriate language parser</li>
     *     <li>Creates elements for classes and methods</li>
     *     <li>Links elements in a hierarchical structure</li>
     * </ol>
     *
     * @param artifact The code artifact to preprocess
     * @return A list of elements containing the original code file and its classes and methods
     */
    protected List<Element> preprocess(Artifact artifact) {
        List<Element> elements = new ArrayList<>();
        Element artifactAsElement =
                new Element(artifact.getIdentifier(), artifact.getType(), artifact.getContent(), 0, null, false);
        elements.add(artifactAsElement);

        var newElements =
                switch (language) {
                    case JAVA -> splitJava(artifactAsElement);
                };

        elements.addAll(newElements);
        return elements;
    }

    /**
     * Splits a Java file into class and method elements using Tree-sitter.
     * This method:
     * <ol>
     *     <li>Parses the Java code using TreeSitterJava</li>
     *     <li>Extracts class bodies from the parse tree</li>
     *     <li>For each class, extracts its methods</li>
     *     <li>Creates elements for classes and methods with appropriate content</li>
     * </ol>
     *
     * @param javaFile The Java file element to split
     * @return A list of elements containing the classes and methods from the file
     */
    private List<Element> splitJava(Element javaFile) {
        List<Element> elements = new ArrayList<>();

        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterJava());

        String content = javaFile.getContent();
        byte[] javaContentInBytes = content.getBytes(StandardCharsets.UTF_8);

        TSTree tree = parser.parseString(null, content);
        var classBodies = parseClassBodies(tree.getRootNode());
        int classStart = 0;
        for (int i = 0; i < classBodies.size(); i++) {
            var classBody = classBodies.get(i);
            var text = new String(Arrays.copyOfRange(javaContentInBytes, classStart, classBody.getStartByte()));
            var classElement = new Element(
                    javaFile.getIdentifier() + SEPARATOR + i, "source code class definition", text, 1, javaFile, false);
            elements.add(classElement);

            int methodStart = classBody.getStartByte();
            var methods = parseMethods(classBody);
            for (int j = 0; j < methods.size(); j++) {
                var method = methods.get(j);
                String methodText =
                        new String(Arrays.copyOfRange(javaContentInBytes, methodStart, method.getEndByte()));
                var methodElement = new Element(
                        classElement.getIdentifier() + SEPARATOR + j,
                        "source code method",
                        methodText,
                        2,
                        classElement,
                        true);
                elements.add(methodElement);
                methodStart = method.getEndByte();
            }
            classStart = classBody.getEndByte();
        }

        return elements;
    }

    /**
     * Recursively extracts method declarations from a Tree-sitter node.
     * This method traverses the parse tree to find all method declarations,
     * which are nodes of type "method_declaration".
     *
     * @param node The Tree-sitter node to search for methods
     * @return A list of Tree-sitter nodes representing method declarations
     */
    private List<TSNode> parseMethods(TSNode node) {
        if (node.getType().equals("method_declaration")) return List.of(node);
        List<TSNode> methods = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            methods.addAll(parseMethods(node.getChild(i)));
        }
        return methods;
    }

    /**
     * Recursively extracts class bodies from a Tree-sitter node.
     * This method traverses the parse tree to find all class bodies,
     * which are nodes of type "class_body".
     *
     * @param node The Tree-sitter node to search for class bodies
     * @return A list of Tree-sitter nodes representing class bodies
     */
    private List<TSNode> parseClassBodies(TSNode node) {
        if (node.getType().equals("class_body")) return List.of(node);
        List<TSNode> classBodies = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            classBodies.addAll(parseClassBodies(node.getChild(i)));
        }
        return classBodies;
    }

    /**
     * Enum representing supported programming languages for method extraction.
     * Currently, only Java is supported.
     */
    public enum Language {
        /** Java programming language */
        JAVA
    }
}
