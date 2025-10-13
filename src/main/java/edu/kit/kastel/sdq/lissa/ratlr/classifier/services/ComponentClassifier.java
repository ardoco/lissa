package edu.kit.kastel.sdq.lissa.ratlr.classifier.services;

import dev.langchain4j.service.Result;

public interface ComponentClassifier {

    Result<Boolean> classify();
}
