package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import java.util.function.Function;

public interface RetrievingFormatter {

    void addRetriever(Function<ReplacementRetriever, ReplacementRetriever> retrieverProvider);
}
