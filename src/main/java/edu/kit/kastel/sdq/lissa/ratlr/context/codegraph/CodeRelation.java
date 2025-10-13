package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import spoon.reflect.declaration.CtType;

import java.util.Collection;
import java.util.List;

public enum CodeRelation {
    INVOKED_TYPES {
        @Override
        public Collection<CtType<?>> getRelated(CtType<?> type) {
            return List.of();
        }
    };

    public abstract Collection<CtType<?>> getRelated(CtType<?> type);
}
