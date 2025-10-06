/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.postprocessor;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;

/**
 * Base class for postprocessors that modify trace link identifiers.
 * This class provides functionality to process trace links based on different
 * module configurations and ID processing strategies.
 * <p>
 * All postprocessors have access to a shared {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} via the protected {@code contextStore} field,
 * which is initialized in the constructor and available to all subclasses.
 * Subclasses should not duplicate context handling or Javadoc for the context parameter.
 * </p>
 * The class supports various types of trace link processing:
 * <ul>
 *     <li>req2code: Requirements to code trace links</li>
 *     <li>sad2code: Software Architecture Documentation to code trace links</li>
 *     <li>sad2sam: Software Architecture Documentation to Software Architecture Model trace links</li>
 *     <li>sam2sad: Software Architecture Model to Software Architecture Documentation trace links</li>
 *     <li>sam2code: Software Architecture Model to code trace links</li>
 *     <li>req2req: Requirements to requirements trace links</li>
 *     <li>identity: No modification to trace links</li>
 * </ul>
 *
 * Each type of processing can have its own specific ID transformation rules,
 * implemented through the {@link IdProcessor} enum.
 */
public class TraceLinkIdPostprocessor {
    /** The ID processor used to transform trace link identifiers */
    private final @Nullable IdProcessor idProcessor;

    /**
     * The shared context store for pipeline components.
     * Available to all subclasses for accessing shared context.
     */
    protected final ContextStore contextStore;

    /**
     * Creates a new postprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected TraceLinkIdPostprocessor(ContextStore contextStore) {
        this.idProcessor = null;
        this.contextStore = Objects.requireNonNull(contextStore);
    }

    /**
     * Creates a new trace link ID postprocessor with the specified ID processor and context store.
     *
     * @param idProcessor The ID processor to use for transforming trace link identifiers
     * @param contextStore The shared context store for pipeline components
     */
    private TraceLinkIdPostprocessor(IdProcessor idProcessor, ContextStore contextStore) {
        this.idProcessor = idProcessor;
        this.contextStore = Objects.requireNonNull(contextStore);
    }

    /**
     * Creates a trace link ID postprocessor based on the module configuration.
     * The type of postprocessor is determined by the module name in the configuration.
     *
     * @param moduleConfiguration The module configuration specifying the type of postprocessor
     * @param contextStore The shared context store for pipeline components
     * @return A new trace link ID postprocessor instance
     * @throws IllegalStateException if the module name is not recognized
     */
    public static TraceLinkIdPostprocessor createTraceLinkIdPostprocessor(
            @Nullable ModuleConfiguration moduleConfiguration, ContextStore contextStore) {

        if (moduleConfiguration == null) {
            return new IdentityPostprocessor(contextStore);
        }

        return switch (moduleConfiguration.name()) {
            case "req2code" -> new TraceLinkIdPostprocessor(IdProcessor.REQ2CODE, contextStore);
            case "sad2code" -> new TraceLinkIdPostprocessor(IdProcessor.SAD2CODE, contextStore);
            case "sad2sam" -> new TraceLinkIdPostprocessor(IdProcessor.SAD2SAM, contextStore);
            case "sam2sad" -> new TraceLinkIdPostprocessor(IdProcessor.SAM2SAD, contextStore);
            case "sam2code" -> new TraceLinkIdPostprocessor(IdProcessor.SAM2CODE, contextStore);
            case "req2req" -> new ReqReqPostprocessor(contextStore);
            case "identity" -> new IdentityPostprocessor(contextStore);
            default -> throw new IllegalStateException("Unexpected value: " + moduleConfiguration.name());
        };
    }

    /**
     * Postprocesses a set of trace links by applying the ID processor to each trace link.
     * This method can be overridden by subclasses to provide custom processing logic.
     *
     * @param traceLinks The set of trace links to process
     * @return A new set containing the processed trace links
     * @throws IllegalStateException if no ID processor is set and the method is not overridden
     */
    public Set<TraceLink> postprocess(Set<TraceLink> traceLinks) {
        if (idProcessor == null) {
            throw new IllegalStateException("idProcessor not set or method not overridden");
        }
        Set<TraceLink> result = new LinkedHashSet<>();
        for (TraceLink traceLink : traceLinks) {
            result.add(idProcessor.process(traceLink));
        }
        return result;
    }
}
