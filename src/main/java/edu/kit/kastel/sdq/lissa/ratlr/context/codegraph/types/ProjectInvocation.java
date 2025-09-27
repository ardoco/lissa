package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.types;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtType;

public record ProjectInvocation(CtInvocation<?> invocation, CtType<?> target) {
}
