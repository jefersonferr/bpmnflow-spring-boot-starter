package org.bpmnflow.starter.dto;

import org.bpmnflow.WorkflowEngine.NextStep;

/**
 * API representation of a {@link NextStep}.
 *
 * <p>Replaces the full {@link org.bpmnflow.model.ActivityNode} in
 * {@code targetActivity} with its abbreviation string, keeping the response
 * consistent with {@link WorkflowRuleDto} and avoiding nested redundancy.</p>
 *
 * <p>Consumers that need the full target activity details can call
 * {@code GET /process/activities/{abbreviation}}.</p>
 *
 * @param targetActivity  target activity abbreviation, or {@code null} for end events
 * @param conclusion      conclusion code that triggers this transition, or {@code null}
 * @param processStatus   resulting process status after the transition, or {@code null}
 * @param ruleType        rule type name, e.g. {@code "SPLIT_TO_TASK"}
 */
public record NextStepDto(
        String targetActivity,
        String conclusion,
        String processStatus,
        String ruleType
) {

    /**
     * Converts a domain {@link NextStep} to its DTO representation.
     *
     * @param step the domain next step to convert; must not be null
     * @return the DTO with the target activity abbreviation
     */
    public static NextStepDto from(NextStep step) {
        return new NextStepDto(
                step.targetActivity() != null ? step.targetActivity().getAbbreviation() : null,
                step.conclusion(),
                step.processStatus(),
                step.ruleType()
        );
    }
}