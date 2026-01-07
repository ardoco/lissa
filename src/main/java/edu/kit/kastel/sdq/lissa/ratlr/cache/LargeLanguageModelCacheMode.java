/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

/**
 * Defines the possible modes of operation that can be cached.
 */
public enum LargeLanguageModelCacheMode {
    /**
     * LargeLanguageModelCacheMode for caching embedding generation operations.
     */
    EMBEDDING,

    /**
     * LargeLanguageModelCacheMode for caching chat-based operations.
     */
    CHAT
}
