/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import java.io.File;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;

/**
 * An embedding creator that uses ONNX models for generating embeddings.
 * This class provides integration with ONNX-based embedding models, allowing
 * for local embedding generation without requiring external services.
 * <p>
 * The creator requires both a model file and a tokenizer file to be present
 * on the local filesystem. These files are specified either through the
 * constructor parameters or through the module configuration.
 * <p>
 * The embedding model uses mean pooling by default for generating the final
 * embeddings from the token-level representations.
 */
public class OnnxEmbeddingCreator extends CachedEmbeddingCreator {
    /**
     * Creates a new ONNX embedding creator with the specified model and file paths.
     *
     * @param model The name of the model
     * @param pathToModel The path to the ONNX model file
     * @param pathToTokenizer The path to the tokenizer file
     */
    public OnnxEmbeddingCreator(String model, String pathToModel, String pathToTokenizer, ContextStore contextStore) {
        super(contextStore, model, 1, pathToModel, pathToTokenizer);
    }

    /**
     * Creates a new ONNX embedding creator from a module configuration.
     * The configuration must specify:
     * <ul>
     *     <li>{@code model}: The name of the model</li>
     *     <li>{@code path_to_model}: The path to the ONNX model file</li>
     *     <li>{@code path_to_tokenizer}: The path to the tokenizer file</li>
     * </ul>
     *
     * @param configuration The configuration containing model and file paths
     * @param contextStore The shared context store for pipeline components
     */
    public OnnxEmbeddingCreator(ModuleConfiguration configuration, ContextStore contextStore) {
        this(
                configuration.argumentAsString("model"),
                configuration.argumentAsString("path_to_model"),
                configuration.argumentAsString("path_to_tokenizer"),
                contextStore);
    }

    /**
     * Creates an ONNX embedding model instance with the specified parameters.
     * The method verifies the existence of both the model and tokenizer files
     * before creating the model instance.
     *
     * @param model The name of the model
     * @param params Additional parameters containing the model and tokenizer file paths
     * @return A configured ONNX embedding model instance
     * @throws IllegalStateException If either the model or tokenizer file does not exist
     */
    @Override
    protected EmbeddingModel createEmbeddingModel(String model, String... params) {
        String modelPath = params[0];
        String tokenizerPath = params[1];

        File modelFile = new File(modelPath);
        File tokenizerFile = new File(tokenizerPath);
        if (!modelFile.exists() || !tokenizerFile.exists()) {
            throw new IllegalStateException("Model or Tokenizer file does not exist");
        }

        PoolingMode poolingMode = PoolingMode.MEAN;
        EmbeddingModel embeddingModel = new OnnxEmbeddingModel(modelFile.toPath(), tokenizerFile.toPath(), poolingMode);
        logger.info("Created OnnxEmbeddingModel with model: {} and tokenizer: {}", modelPath, tokenizerPath);
        return embeddingModel;
    }
}
