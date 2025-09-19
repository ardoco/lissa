package edu.kit.kastel.sdq.lissa.ratlr.codegraph.types;

import java.util.List;

public record TargetsContainer(List<ProjectInvocation> projectInvocations, List<OuterInvocation> outerInvocations) {
}
