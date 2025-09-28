package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

public interface ValueFormatter<T> extends Formatter {
    
    void setValue(T value);
}
