package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.token;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Creates and stores an analysis between the texts that have been entered.
 *
 * @author Programmieren-Team
 */
public class Analysis {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Collection<Sequence> targetSequences;
    private final Tokenization tokenization;
    private final int mml;
    private final int threads = 100;
    private final double exponent;

    /**
     * Creates a new analysis.
     * @param tokenization The type of analyzation
     * @param mml The minimum size a match must have (minimum match length)
     */
    public Analysis(Tokenization tokenization, int mml, double exponent) {
        this.tokenization = tokenization;
        this.mml = mml;
        this.exponent = exponent;
        this.targetSequences = new ArrayList<>();
    }
    
    private synchronized Collection<Sequence> getTargetSequences() {
        return new ArrayList<>(this.targetSequences);
    }

    public synchronized void addTargets(List<Pair<Element, float[]>> allElementsInStore) {
        if (!this.targetSequences.isEmpty()) {
            return;
        }
        
        this.targetSequences.addAll(allElementsInStore.stream()
                .map(Pair::first)
                .map(element -> new Sequence(element, this.tokenization.tokenize(element.getContent())))
                .toList());
    }
    
    public List<SequenceAnalysis> analyzeParallel(Element source) {
        String[] tokenizedSource = this.tokenization.tokenize(source.getContent());
        Sequence sourceSequence = new Sequence(source, tokenizedSource);
        ConcurrentLinkedQueue<SequenceAnalysis> results = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Pair<Sequence, Sequence>> taskQueue = new ConcurrentLinkedQueue<>(getTargetSequences().stream()
                .map(target -> new Pair<>(sourceSequence, target))
                .toList());

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = Thread.ofVirtual().start(new Runnable() {
                private final double exponent = Analysis.this.exponent;
                private final int mml = Analysis.this.mml;

                @Override
                public void run() {
                    while (!taskQueue.isEmpty()) {
                        Pair<Sequence, Sequence> pair = taskQueue.poll();
                        if (pair == null) {
                            return;
                        }
                        SequenceAnalysis sequenceAnalysis = new SequenceAnalysis(pair.first(), pair.second(), this.mml, this.exponent);
                        var result = sequenceAnalysis.compareTokens();
                        logger.debug(
                                "Similarity (P) {} with {}: {}",
                                pair.first().getElement().getIdentifier(),
                                pair.second().getElement().getIdentifier(),
                                result);
                        results.add(sequenceAnalysis);
                    }
                }
            });
        }

        logger.info("Waiting for similarity calculation to finish. Tasks in queue: {}", taskQueue.size());

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                logger.error("Worker thread interrupted.", e);
                Thread.currentThread().interrupt();
            }
        }

        List<SequenceAnalysis> sorted = new ArrayList<>(results);
        logger.info("Finished parallel tokenized similarity with {} results.", sorted.size());
        return sorted;
    }
}
