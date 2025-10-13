package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SimpleComponent extends Component {
    
    private final String simpleName;
    private final String qualifiedName;
    private final SortedSet<Artifact> containedArtifacts = new TreeSet<>(Comparator.comparing(Artifact::getIdentifier));

    public SimpleComponent(String simpleName, String qualifiedName) {
        this.simpleName = simpleName;
        this.qualifiedName = qualifiedName;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public SortedSet<Artifact> getContainedArtifacts() {
        return containedArtifacts;
    }

    public final void determineContainedArtifacts(Collection<Artifact> providedValues) {
        containedArtifacts.addAll(determineArtifacts(providedValues));
    }
    
    protected abstract Collection<Artifact> determineArtifacts(Collection<Artifact> providedValues);
}
