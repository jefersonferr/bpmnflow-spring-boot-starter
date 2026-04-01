package org.bpmnflow;

import org.bpmnflow.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link WorkflowEngine}.
 *
 * <p>Wraps a parsed {@link Workflow} and provides navigation logic on top of it.
 * This class is thread-safe: the workflow is immutable after construction and all
 * methods derive their results from it without mutation.
 *
 * <p>The activity index is built once at construction time to keep
 * {@link #findActivity(String)} and {@link #nextSteps(String)} O(1) / O(rules).
 */
public class WorkflowEngineImpl implements WorkflowEngine {

    private final Workflow workflow;

    /**
     * Index: abbreviation (e.g. "TR-TR1") → ActivityNode.
     * Built once at construction; lookup is O(1).
     */
    private final Map<String, ActivityNode> activityIndex;

    public WorkflowEngineImpl(Workflow workflow) {
        Objects.requireNonNull(workflow, "workflow must not be null");
        this.workflow = workflow;
        this.activityIndex = workflow.getActivities().stream()
                .collect(Collectors.toMap(ActivityNode::getAbbreviation, Function.identity()));
    }

    @Override
    public Workflow getWorkflow() {
        return workflow;
    }

    @Override
    public List<Inconsistency> validate() {
        return List.copyOf(workflow.getInconsistencies());
    }

    @Override
    public boolean isValid() {
        return workflow.getInconsistencies().isEmpty();
    }

    @Override
    public List<ActivityNode> listActivities() {
        return List.copyOf(workflow.getActivities());
    }

    @Override
    public List<Stage> listStages() {
        return List.copyOf(workflow.getStages());
    }

    @Override
    public List<WorkflowRule> listRules() {
        return List.copyOf(workflow.getRules());
    }

    /**
     * Resolves every outgoing transition from the given activity.
     *
     * <p>A rule is "outgoing from X" when its source abbreviation matches X.
     * Rules with a null source (START_TO_TASK) are entry points, not outgoing transitions.
     *
     * <p>The result includes:
     * <ul>
     *   <li>Direct Task→Task and Task→EndEvent transitions</li>
     *   <li>Split branches (Task→Split→Task, Task→Split→End)</li>
     *   <li>Merge paths (Task→Merge→Task, Task→Merge→End)</li>
     * </ul>
     */
    @Override
    public List<NextStep> nextSteps(String activityAbbreviation) {
        findActivity(activityAbbreviation); // validates existence, throws if not found

        return workflow.getRules().stream()
                .filter(rule -> rule.getSource() != null)
                .filter(rule -> activityAbbreviation.equals(rule.getSource().getAbbreviation()))
                .map(rule -> new NextStep(
                        rule.getTarget(),
                        rule.getConclusion() != null ? rule.getConclusion().getCode() : null,
                        rule.getProcessStatus(),
                        rule.getType().name()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Finds rules triggered by a specific process status.
     *
     * <p>This covers two cases:
     * <ul>
     *   <li>START_TO_TASK rules — where the StartEvent carries the given status</li>
     *   <li>Any rule whose processStatus matches — useful for finding entry points after
     *       an external system pushes a status update</li>
     * </ul>
     */
    @Override
    public List<WorkflowRule> rulesTriggeredBy(String processStatus) {
        if (processStatus == null || processStatus.isBlank()) {
            throw new IllegalArgumentException("processStatus must not be blank");
        }
        return workflow.getRules().stream()
                .filter(rule -> processStatus.equals(rule.getProcessStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public ActivityNode findActivity(String abbreviation) {
        ActivityNode node = activityIndex.get(abbreviation);
        if (node == null) {
            throw new IllegalArgumentException(
                    "Activity not found: '" + abbreviation + "'. Available: " + activityIndex.keySet()
            );
        }
        return node;
    }
}
