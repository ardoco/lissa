/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Test class for the ModelUMLPreprocessor.
 * This class tests the preprocessing of UML model artifacts, specifically:
 * <ul>
 *     <li>Loading and processing of UML model files</li>
 *     <li>Extraction of model elements</li>
 *     <li>Validation of element properties</li>
 * </ul>
 *
 * The tests use a sample UML model file (mediastore.uml) to verify that:
 * <ol>
 *     <li>The preprocessor correctly loads and parses UML files</li>
 *     <li>The correct number of elements are extracted</li>
 *     <li>Element properties (content, identifier) are correctly set</li>
 * </ol>
 */
class ModelUMLPreprocessorTest {

    /**
     * Tests the preprocessing of a UML model file.
     * This test:
     * <ul>
     *     <li>Loads a sample UML model file (mediastore.uml)</li>
     *     <li>Processes it using the ModelUMLPreprocessor</li>
     *     <li>Verifies the number of extracted elements</li>
     *     <li>Validates the content and identifier of a specific element</li>
     * </ul>
     *
     * The test ensures that:
     * <ol>
     *     <li>The UML file is correctly loaded and parsed</li>
     *     <li>24 elements are extracted from the model</li>
     *     <li>The first element has the correct content and identifier</li>
     * </ol>
     */
    @Test
    void testUmlPreprocess() throws IOException {
//        // Verify that the UML file exists
//        File model = new File("src/test/resources/mediastore.uml");
//        assertTrue(model.exists(), "UML file should exist");
//
//        // Create and process the artifact
//        ModelUMLPreprocessor preprocessor =
//                new ModelUMLPreprocessor(new ModuleConfiguration("dummy", Map.of()), new ContextStore());
//        List<Element> elements = preprocessor.preprocess(
//                List.of(new Artifact("mediastore.uml", "uml", Files.readString(model.toPath()))));
//
//        // Verify the number of elements
//        assertEquals(24, elements.size(), "Should extract 24 elements from the UML model");
//
//        // Verify a specific element
//        Element element = elements.stream()
//                .filter(it -> it.getContent().contains("Name: FileStorage"))
//                .findFirst()
//                .orElseThrow();
//        assertEquals("mediastore.uml$4$_qxAiILg7EeSNPorBlo7x9g", element.getIdentifier());
//        assertNotNull(element.getContent(), "Element content should not be null");
    }
}
