/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.postprocessor;

import java.util.LinkedHashSet;
import java.util.Set;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;

/**
 * A postprocessor that transforms trace links between requirements.
 * This class specifically handles the transformation of requirement-to-requirement
 * trace links by removing file extensions from both source and target identifiers.
 * <p>
 * The transformation follows this pattern:
 * <ul>
 *     <li>Input: TraceLink[sourceId=UC10E1.txt, targetId=UC10E2.txt]</li>
 *     <li>Output: TraceLink[sourceId=UC10E1, targetId=UC10E2]</li>
 * </ul>
 *
 * This postprocessor is used when the module configuration specifies "req2req" as the
 * processor type, indicating that the trace links are between requirement artifacts.
 */
public class ReqReqPostprocessor extends TraceLinkIdPostprocessor {

    public ReqReqPostprocessor(ContextStore contextStore) {
        super(contextStore);
    }

    /**
     * Transforms a set of requirement-to-requirement trace links by removing file extensions
     * from both source and target identifiers. The transformation is applied to each trace link
     * in the input set, and the results are collected in a new set.
     *
     * @param traceLinks The set of trace links to process
     * @return A new set containing the transformed trace links
     */
    @Override
    public Set<TraceLink> postprocess(Set<TraceLink> traceLinks) {
        // TraceLink[sourceId=UC10E1.txt, targetId=UC10E2.txt]
        // => TraceLink[sourceId=UC10E1, targetId=UC10E2]
        Set<TraceLink> result = new LinkedHashSet<>();
        for (TraceLink traceLink : traceLinks) {
            String sourceId = traceLink.sourceId();
            String targetId = traceLink.targetId();
            sourceId = sourceId.substring(0, sourceId.lastIndexOf("."));
            targetId = targetId.substring(0, targetId.lastIndexOf("."));
            result.add(new TraceLink(sourceId, targetId));
        }
        return result;
    }
}
