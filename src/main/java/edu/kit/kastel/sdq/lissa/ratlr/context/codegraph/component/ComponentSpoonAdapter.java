package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

public class ComponentSpoonAdapter implements Component, TypeContainer {
    
    private final CtPackage rootPackage;
    private final Collection<CtType<?>> containedTypes;
    private final SortedSet<String> packagePaths;
    private final Collection<CtType<?>> providedInterfaces;
    private final Collection<CtExecutableReference<?>> providedInterfaceMethods;
    private final SortedSet<Artifact> containedArtifacts;

    public ComponentSpoonAdapter(CtPackage rootPackage, Collection<CtType<?>> containedTypes, SortedSet<Artifact> containedArtifacts, SortedSet<String> packagePaths, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>> ctPackages) {
        this.rootPackage = rootPackage;
        this.containedTypes = containedTypes;
        this.packagePaths = packagePaths;
        this.containedArtifacts = containedArtifacts;
        this.providedInterfaceMethods = null;
        this.providedInterfaces = null;
    }

    public CtPackage getRootPackage() {
        return rootPackage;
    }

    @Override
    public Collection<CtType<?>> getContainedTypes() {
        return containedTypes;
    }

    @Override
    public String getSimpleName() {
        return rootPackage.getSimpleName();
    }
    
    @Override
    public String getQualifiedName() {
        return rootPackage.getQualifiedName();
    }

    @Override
    public SortedSet<Artifact> getContainedArtifacts() {
        return Collections.unmodifiableSortedSet(containedArtifacts);
    }

    @Override
    public SortedSet<String> getPaths() {
        return Collections.unmodifiableSortedSet(packagePaths);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ComponentSpoonAdapter component = (ComponentSpoonAdapter) o;
        return rootPackage.equals(component.rootPackage);
    }

    @Override
    public int hashCode() {
        return rootPackage.hashCode();
    }
}
