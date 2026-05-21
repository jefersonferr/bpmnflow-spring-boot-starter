package org.bpmnflow.starter.dto;

import org.bpmnflow.model.WorkflowRule;

/**
 * API representation of a {@link WorkflowRule}.
 *
 * <p>Replaces the full {@link org.bpmnflow.model.ActivityNode} objects in
 * {@code source} and {@code target} with their abbreviations (e.g. {@code "TR-ADR"}),
 * keeping the response lean and avoiding redundant nested objects.</p>
 *
 * <p>Consumers that need the full activity details can call
 * {@code GET /process/activities/{abbreviation}}.</p>
 *
 * @param type          rule type name, e.g. {@code "SPLIT_TO_TASK"}
 * @param source        source activity abbreviation, or {@code null} for entry rules
 * @param target        target activity abbreviation, or {@code null} for terminal rules
 * @param conclusion    conclusion code that triggers this transition, or {@code null}
 * @param processStatus resulting process status after the transition, or {@code null}
 */
public record WorkflowRuleDto(
        String type,
        String source,
        String target,
        String conclusion,
        String processStatus
) {

    /**
     * Converts a domain {@link WorkflowRule} to its DTO representation.
     *
     * @param rule the domain rule to convert; must not be null
     * @return the DTO with abbreviations in place of full activity objects
     */
    public static WorkflowRuleDto from(WorkflowRule rule) {
        return new WorkflowRuleDto(
                rule.getType() != null ? rule.getType().name() : null,
                rule.getSource() != null ? rule.getSource().getAbbreviation() : null,
                rule.getTarget() != null ? rule.getTarget().getAbbreviation() : null,
                rule.getConclusion() != null ? rule.getConclusion().getCode() : null,
                rule.getProcessStatus()
        );
    }
}