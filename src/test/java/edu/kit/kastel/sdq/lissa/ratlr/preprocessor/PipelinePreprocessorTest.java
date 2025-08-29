package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import org.junit.jupiter.api.Test;

/**
 * Test class for the PipelinePreprocessor.
 * This class tests the preprocessing of artifacts with multiple sequential preprocessors, specifically:
 * <ul>
 *     <li>Loading and processing of multiple preprocessors</li>
 *     <li>Extraction of sequential elements</li>
 *     <li>Validation of sequential element properties</li>
 * </ul>
 *
 * The tests verify that:
 * <ol>
 *     <li>The preprocessor correctly loads and processes artifacts sequentially</li>
 *     <li>Elements of intermediate preprocessors are not flagged for classification</li>
 *     <li>The preprocessor can load any other preprocessor including itself</li>
 *     <li>Having a single stage produces the exact same results as using that preprocessor directly</li>
 * </ol>
 */
public class PipelinePreprocessorTest {

    private static final String PIPELINE_MULTIPLE = """
          {
            "name": "pipeline",
            "args": {
              "compare_intermediate" : "false",
              "preprocessors" : [
                {
                  "name" : "text_replacer",
                  "args" : {
                    "placeholder" : "<<<%s>>>",
                    "template" : "{"id":"<<<element_identifier>>>","rbnslfnkse":"<<<element_content>>>"}"
                  }
                },
                {
                  "name" : "json_array_splitter",
                  "args" : {
                    "remapper" : {
                      "identification_tasks" : "identification_task"
                    }
                  }
                },
                {
                  "name" : "json_converter_text",
                  "args" : {
                    "replace_format" : "<<<%s>>>",
                    "template" : "<<<sentence>>>\\n\\n<<<identification_task>>>"
                  }
                }
              ]
            }
          }
            """;
    
    @Test
    public void testPreprocessorLoading() {
        
    }
}
