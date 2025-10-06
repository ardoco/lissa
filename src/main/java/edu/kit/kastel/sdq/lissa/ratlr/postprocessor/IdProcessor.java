/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.postprocessor;

import java.util.function.UnaryOperator;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;

/**
 * Enum representing different strategies for processing trace link identifiers.
 * Each enum value defines how source and target identifiers should be transformed
 * based on the type of trace link being processed.
 * <p>
 * The enum supports the following processing strategies:
 * <ul>
 *     <li>REQ2CODE: Processes requirement-to-code trace links by removing file extensions</li>
 *     <li>SAD2CODE: Processes Software Architecture Documentation to code trace links</li>
 *     <li>SAM2SAD: Processes Software Architecture Model to Software Architecture Documentation trace links</li>
 *     <li>SAD2SAM: Processes Software Architecture Documentation to Software Architecture Model trace links</li>
 *     <li>SAM2CODE: Processes Software Architecture Model to code trace links</li>
 * </ul>
 *
 * Each strategy consists of two parts:
 * <ul>
 *     <li>A source ID processor that transforms the source identifier</li>
 *     <li>A target ID processor that transforms the target identifier</li>
 * </ul>
 */
enum IdProcessor {
    /** Processes requirement-to-code trace links by removing file extensions from both identifiers */
    REQ2CODE(
            sourceId -> sourceId.substring(0, sourceId.indexOf(".")),
            targetId -> targetId.substring(0, targetId.indexOf("."))),
    /** Processes Software Architecture Documentation to code trace links */
    SAD2CODE(IdProcessor::processSAD, targetId -> targetId),
    /** Processes Software Architecture Model to Software Architecture Documentation trace links */
    SAM2SAD(IdProcessor::processSAM, IdProcessor::processSAD),
    /** Processes Software Architecture Documentation to Software Architecture Model trace links */
    SAD2SAM(IdProcessor::processSAD, IdProcessor::processSAM),
    /** Processes Software Architecture Model to code trace links */
    SAM2CODE(IdProcessor::processSAM, targetId -> targetId);

    /** The function that processes source identifiers */
    private final UnaryOperator<String> sourceIdProcessor;
    /** The function that processes target identifiers */
    private final UnaryOperator<String> targetIdProcessor;

    /**
     * Creates a new ID processor with the specified source and target processors.
     *
     * @param sourceIdProcessor The function that processes source identifiers
     * @param targetIdProcessor The function that processes target identifiers
     */
    IdProcessor(UnaryOperator<String> sourceIdProcessor, UnaryOperator<String> targetIdProcessor) {
        this.sourceIdProcessor = sourceIdProcessor;
        this.targetIdProcessor = targetIdProcessor;
    }

    /**
     * Processes a trace link by applying the source and target ID processors.
     * Creates a new trace link with the processed identifiers.
     *
     * @param traceLink The trace link to process
     * @return A new trace link with processed identifiers
     */
    public TraceLink process(TraceLink traceLink) {
        return new TraceLink(
                sourceIdProcessor.apply(traceLink.sourceId()), targetIdProcessor.apply(traceLink.targetId()));
    }

    /**
     * Processes a Software Architecture Documentation identifier.
     * Extracts the numeric part after the last '$' character and increments it by 1.
     *
     * @param sadID The SAD identifier to process
     * @return The processed identifier
     */
    private static String processSAD(String sadID) {
        return String.valueOf(Integer.parseInt(sadID.substring(sadID.lastIndexOf("$") + 1)) + 1);
    }

    /**
     * Processes a Software Architecture Model identifier.
     * Extracts the part after the last '$' character.
     *
     * @param samID The SAM identifier to process
     * @return The processed identifier
     */
    private static String processSAM(String samID) {
        return samID.substring(samID.lastIndexOf("$") + 1);
    }
}
