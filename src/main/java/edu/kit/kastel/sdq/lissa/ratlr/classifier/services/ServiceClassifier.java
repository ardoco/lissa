package edu.kit.kastel.sdq.lissa.ratlr.classifier.services;

import dev.langchain4j.service.Result;

public interface ServiceClassifier {

    Result<Boolean> classify(String request);
}
