/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

/**
 * Defines the available prompt templates for classification.
 */
public enum ReasoningClassifierPrompt {
    /**
     * Basic prompt that asks for reasoning about trace links.
     */
    REASON_WITH_NAME(
            "Below are two artifacts from the same software system. Is there a traceability link between (1) and (2)? Give your reasoning and then answer with 'yes' or 'no' enclosed in <trace> </trace>.\n (1) {source_type}: '''{source_content}''' \n (2) {target_type}: '''{target_content}''' "),

    /**
     * Prompt that asks for conceivable trace links.
     */
    REASON_WITH_NAME_CONCEIVABLE(
            "Below are two artifacts from the same software system. Is there a conceivable traceability link between (1) and (2)? Give your reasoning and then answer with 'yes' or 'no' enclosed in <trace> </trace>.\n (1) {source_type}: '''{source_content}''' \n (2) {target_type}: '''{target_content}''' "),

    /**
     * Prompt that requires high certainty for positive responses.
     */
    REASON_WITH_NAME_YES_IF_CERTAIN(
            "Below are two artifacts from the same software system.\n Is there a traceability link between (1) and (2)? Give your reasoning and then answer with 'yes' or 'no' enclosed in <trace> </trace>. Only answer yes if you are absolutely certain.\n (1) {source_type}: '''{source_content}''' \n (2) {target_type}: '''{target_content}''' ");

    /**
     * The template string for this prompt.
     */
    private final String promptTemplate;

    /**
     * Creates a new prompt with the specified template.
     *
     * @param promptTemplate The template string for the prompt
     */
    ReasoningClassifierPrompt(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }
}
