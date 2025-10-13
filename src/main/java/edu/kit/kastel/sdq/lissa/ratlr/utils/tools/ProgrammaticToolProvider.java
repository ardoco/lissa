package edu.kit.kastel.sdq.lissa.ratlr.utils.tools;

import java.lang.reflect.Method;
import java.util.List;

public interface ProgrammaticToolProvider {
    
    List<Method> getToolMethods() throws NoSuchMethodException;
    
}
