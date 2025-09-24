package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.InvokedTypesCollector;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.OuterInvocation;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.ProjectInvocation;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.TargetsContainer;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;

import java.util.List;
import java.util.Map;

public class InvocationCollectorTest {
    
    @Test
    public void onInvocationCollecting() {
        SpoonAPI launcher = new Launcher();
        launcher.addInputResource("datasets/doc2code/TeaStore/model_2022/code");
        CtModel model = launcher.buildModel();
        InvokedTypesCollector invocationCollector = new InvokedTypesCollector();
        model.processWith(invocationCollector);

        List<String> invokedProjectTypes = invocationCollector.getInvokedTypes().entrySet().stream()
                .filter(entry -> entry.getKey().getDeclaringType().getSimpleName().equals("CartServlet"))
                .flatMap(entry -> entry.getValue().projectInvocations().stream().map(ProjectInvocation::target))
                .map(ctType -> {
                    if (ctType == null) {
                        System.out.println("nulling");
                        return "null";
                    } else {
                        return ctType.getSimpleName();
                    }
                })
                .toList();
        System.out.println(invokedProjectTypes);
        
        List<String> invokedOuterTypes = invocationCollector.getInvokedTypes().entrySet().stream()
                .filter(entry -> entry.getKey().getDeclaringType().getSimpleName().equals("CartServlet"))
                .flatMap(entry -> entry.getValue().outerInvocations().stream().map(OuterInvocation::target))
                .map(ctType -> {
                    if (ctType == null) {
                        System.out.println("nulling");
                        return "null";
                    } else {
                        return ctType.getSimpleName();
                    }
                })
                .toList();
        System.out.println(invokedOuterTypes);
    }
    
    private static List<String> getInvokedTypesTransitivelyBFS(Map<TypeDeclaration, TargetsContainer> invokedTypes, String simpleName) {
        List<TypeDeclaration> list = invokedTypes.keySet().stream()
                .filter(targetsContainer -> targetsContainer.getDeclaringType().getSimpleName().equals(simpleName))
                .toList();
        for (TypeDeclaration declaration : list) {
            
        }
        return null;
    }
}
