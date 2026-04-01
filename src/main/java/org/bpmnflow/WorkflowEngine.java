package org.bpmnflow;

import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Inconsistency;
import org.bpmnflow.model.Stage;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.model.WorkflowRule;

import java.util.List;

/**
 * The core service of the BPMNFlow starter.
 *
 * <p>Provides navigation and inspection of a parsed BPMN {@link Workflow}.
 * All methods are read-only — the engine is stateless and thread-safe.
 *
 * <p>Typical usage:
 * <pre>
 * // 1. Check model health
 * List&lt;Inconsistency&gt; problems = engine.validate();
 *
 * // 2. Discover what comes after a given activity
 * List&lt;NextStep&gt; next = engine.nextSteps("TR-TR1");
 *
 * // 3. Filter rules triggered by a specific process status
 * List&lt;WorkflowRule&gt; entryRules = engine.rulesTriggeredBy("NV");
 * </pre>
 */
public interface WorkflowEngine {

    /**
     * Returns the full parsed {@link Workflow} object.
     * Useful for inspection and serialization.
     */
    Workflow getWorkflow();

    /**
     * Returns all validation inconsistencies detected during parsing.
     * An empty list means the model is fully consistent with its config.
     */
    List<Inconsistency> validate();

    /**
     * Returns true if the model has zero inconsistencies.
     */
    boolean isValid();

    /**
     * Returns all activities in the workflow, ordered as parsed.
     */
    List<ActivityNode> listActivities();

    /**
     * Returns all stages declared in the workflow lanes.
     */
    List<Stage> listStages();

    /**
     * Returns all workflow rules (transitions between activities).
     */
    List<WorkflowRule> listRules();

    /**
     * Given an activity abbreviation (e.g. "TR-TR1"), returns every
     * possible next step reachable from it, including the conclusion
     * and process status that would result from each transition.
     *
     * @param activityAbbreviation the {@code stageCode-activityCode} string
     * @return list of next steps; empty if the activity is a terminal node
     * @throws IllegalArgumentException if the abbreviation is not found
     */
    List<NextStep> nextSteps(String activityAbbreviation);

    /**
     * Returns all rules whose source process_status matches the given value.
     * Useful for finding which activity should start when a case enters a given status.
     *
     * @param processStatus the process status string (e.g. "NV", "EM_ANALISE")
     */
    List<WorkflowRule> rulesTriggeredBy(String processStatus);

    /**
     * Finds an activity by its abbreviation ({@code stageCode-activityCode}).
     *
     * @throws IllegalArgumentException if not found
     */
    ActivityNode findActivity(String abbreviation);

    /**
     * Summary record returned by {@link #nextSteps(String)}.
     *
     * @param targetActivity  the activity to transition to (null for end events)
     * @param conclusion      the conclusion label/code that triggers this transition (may be null)
     * @param processStatus   the resulting process status after the transition (may be null)
     * @param ruleType        the rule type name (e.g. "TASK_TO_TASK", "SPLIT_TO_TASK")
     */
    record NextStep(
            ActivityNode targetActivity,
            String conclusion,
            String processStatus,
            String ruleType
    ) {}
}
