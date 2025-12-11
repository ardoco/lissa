/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

public interface CacheParameter {
    /**
     * Provide a unique string based on the actual cache parameters (for the file name of LocalCache)
     * @return a unique string based on the cache parameters
     */
    String parameters();
}
