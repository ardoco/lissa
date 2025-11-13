/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.optimizer;

/**
 * Immutable record holding the result of a prompt generation step.
 * Stores both the AI-generated prompt and the LiSSA-specific prompt.
 *
 * @param aiPrompt    the prompt intended for the AI model
 * @param lissaPrompt the prompt formatted for LiSSA
 */
public record PromptGenerationResult(String aiPrompt, String lissaPrompt) {}
