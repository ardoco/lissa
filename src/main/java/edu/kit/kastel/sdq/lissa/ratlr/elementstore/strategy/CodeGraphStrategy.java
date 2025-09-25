package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.List;

public class CodeGraphStrategy implements RetrievalStrategy {

    private final ContextStore contextStore;

    public CodeGraphStrategy(ContextStore contextStore) {
        this.contextStore = contextStore;
        if (!contextStore.hasContext(CodeGraphProvider.CONTEXT_CODE_PATH)) {
            throw new IllegalStateException("illegal artifact provider, must be 'code'");
        }
    }
    
    @Override
    public List<Pair<Element, Float>> findSimilarElements(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        return List.of();
    }
}
