package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token.Analysis;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token.SequenceAnalysis;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token.Tokenization;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class TokenSimilarity extends MaxResultsStrategy {

    private final Tokenization tokenization;
    private final int mml;
    private final double exponent;

    public TokenSimilarity(ModuleConfiguration configuration) {
        super(configuration);
        this.tokenization = Tokenization.valueOf(configuration.argumentAsString("tokenization", "WORD"));
        this.mml = configuration.argumentAsInt("minimum_match_length", 1);
        this.exponent = configuration.argumentAsDouble("exponent", 2.0);
    }

    @Override
    public List<Pair<Element, Float>> findSimilarElementsInternal(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        Analysis analysis = new Analysis(tokenization, mml, exponent);
        analysis.addTargets(new ArrayList<>(allElementsInStore));
        List<SequenceAnalysis> analyzed = analysis.analyzeParallel(query.first());
        List<Pair<Element, Float>> results = new ArrayList<>(analyzed.size());
        for (SequenceAnalysis sequenceAnalysis : analyzed) {
            results.add(new Pair<>(sequenceAnalysis.getOther().getElement(), (float) sequenceAnalysis.getScore()));
        }
        return results;
    }
}
