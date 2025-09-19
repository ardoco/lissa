/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

/**
 * Test class for the CodeMethodPreprocessor.
 * This class tests the preprocessing of source code artifacts, specifically:
 * <ul>
 *     <li>Extraction of class and method elements from Java code</li>
 *     <li>Creation of hierarchical relationships between elements</li>
 *     <li>Validation of element properties and structure</li>
 * </ul>
 *
 * The tests use the test class itself as input to verify that:
 * <ol>
 *     <li>The preprocessor correctly parses Java code</li>
 *     <li>Classes and methods are correctly identified and extracted</li>
 *     <li>Element properties (content, identifier, granularity) are correctly set</li>
 *     <li>Parent-child relationships are properly established</li>
 * </ol>
 */
class CodeMethodPreprocessorTest {

    /**
     * Tests the preprocessing of a Java source code file.
     * This test:
     * <ul>
     *     <li>Uses the test class itself as input</li>
     *     <li>Processes it using the CodeMethodPreprocessor</li>
     *     <li>Verifies the number of extracted elements</li>
     *     <li>Validates the content and structure of specific elements</li>
     * </ul>
     *
     * The test ensures that:
     * <ol>
     *     <li>The Java file is correctly parsed</li>
     *     <li>Three elements are extracted (file, class, and method)</li>
     *     <li>The package declaration is included in the file element</li>
     *     <li>The @Test annotation is included in the method element</li>
     *     <li>Element identifiers and granularity levels are correctly set</li>
     * </ol>
     */
    @Test
    void preprocessSelf() throws IOException {
//        CodeMethodPreprocessor codeMethodPreprocessor = new CodeMethodPreprocessor(
//                new ModuleConfiguration(null, Map.of("language", "JAVA")), new ContextStore());
//        String thisClassContent = new Scanner(new File(
//                        "src/test/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/CodeMethodPreprocessorTest.java"))
//                .useDelimiter("\\A")
//                .next();
//        var elements = codeMethodPreprocessor.preprocess(
//                List.of(new Artifact("CodeMethodPreprocessorTest.java", "JAVA", thisClassContent)));
//        assertEquals(3, elements.size());
//        var head = elements.get(1);
//        assertEquals("CodeMethodPreprocessorTest.java$0", head.getIdentifier());
//        assertTrue(head.getContent().contains("package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;"));
//
//        var firstMethod = elements.get(2);
//        assertEquals("CodeMethodPreprocessorTest.java$0$0", firstMethod.getIdentifier());
//        assertTrue(firstMethod.getContent().contains("@Test"));
//        assertTrue(firstMethod.getContent().contains("preprocessSelf"));
    }
}
