package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;

import java.io.File;

public abstract class PathedProvider extends ArtifactProvider {

    /**
     * The file or directory path from which artifacts are loaded.
     */
    protected final File path;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected PathedProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.path = new File(configuration.argumentAsString("path"));
        if (!path.exists()) {
            throw new IllegalArgumentException("Path does not exist: " + path.getAbsolutePath());
        }
    }
}
