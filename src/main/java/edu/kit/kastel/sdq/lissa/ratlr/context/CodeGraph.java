package edu.kit.kastel.sdq.lissa.ratlr.context;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.nio.file.Path;
import java.util.Collection;

public interface CodeGraph extends Context {

    void initializeComponentExtraction(ModuleConfiguration configuration, ContextStore contextStore);

    Collection<Component> getComponents();

    Collection<Artifact> getArtifacts();

    Path getCodeRoot();
    
}
