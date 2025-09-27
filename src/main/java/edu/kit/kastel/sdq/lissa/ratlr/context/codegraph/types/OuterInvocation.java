package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.types;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtTypeReference;

public record OuterInvocation(CtInvocation<?> invocation, CtTypeReference<?> target) {
}
