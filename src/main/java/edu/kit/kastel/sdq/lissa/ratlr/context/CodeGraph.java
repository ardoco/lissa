package edu.kit.kastel.sdq.lissa.ratlr.context;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.Collection;

public interface CodeGraph extends Context {

    Collection<Component> getComponents(ComponentStrategy[] strategies);

    Collection<Artifact> getArtifacts();
    
}
