package edu.kit.kastel.sdq.lissa.ratlr.codegraph.types;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class InvokedTypesCollector extends AbstractProcessor<CtInvocation<?>> {
    
    private final Map<TypeDeclaration, TargetsContainer> invokedTypesByDeclaringType = new HashMap<>();
    private final Map<TypeDeclaration, TargetsContainer> invokedTypesView = Collections.unmodifiableMap(invokedTypesByDeclaringType);

    public Map<TypeDeclaration, TargetsContainer> getInvokedTypes() {
        return invokedTypesView;
    }

    @Override
    public void process(CtInvocation<?> element) {
        CtType<?> declaringParent = element.getParent(CtType.class);
        if (declaringParent == null) {
            throw new RuntimeException("invocation has no CtType parent: " + element);
        }
        TypeDeclaration declaration = new TypeDeclaration(declaringParent);
        invokedTypesByDeclaringType.putIfAbsent(declaration, new TargetsContainer(new LinkedList<>(), new LinkedList<>()));
        if (element.getExecutable() != null) {
            CtExecutable<?> targetDeclaration = element.getExecutable().getExecutableDeclaration();
            if (targetDeclaration == null) {
                CtExpression<?> target = element.getTarget();
                if (target != null) {
                    CtTypeReference<?> type = target.getType();
                    if (type != null) {
                        invokedTypesByDeclaringType.get(declaration).outerInvocations().add(new OuterInvocation(element, type));
                    }
                } else {
                    invokedTypesByDeclaringType.get(declaration).outerInvocations().add(new OuterInvocation(element, element.getExecutable().getType()));
                }
            } else {
                invokedTypesByDeclaringType.get(declaration).projectInvocations().add(new ProjectInvocation(element
                        , targetDeclaration.getParent(CtType.class)));
            }
        }
    }
}
