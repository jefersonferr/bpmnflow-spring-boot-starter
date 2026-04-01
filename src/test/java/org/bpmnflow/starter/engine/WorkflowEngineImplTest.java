package org.bpmnflow.starter.engine;

import org.bpmnflow.WorkflowEngine.NextStep;
import org.bpmnflow.WorkflowEngineImpl;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Conclusion;
import org.bpmnflow.model.Inconsistency;
import org.bpmnflow.model.RuleType;
import org.bpmnflow.model.Stage;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.model.WorkflowRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowEngineImpl")
class WorkflowEngineImplTest {

    private Workflow workflow;
    private WorkflowEngineImpl engine;

    private ActivityNode activity(String stage, String code, String name) {
        return new ActivityNode(stage, code, name, null);
    }

    private WorkflowRule rule(RuleType type, ActivityNode source, ActivityNode target, String status) {
        return new WorkflowRule(type, source, target, null, status);
    }

    private WorkflowRule ruleWithConclusion(RuleType type, ActivityNode source, ActivityNode target,
                                            Conclusion conclusion, String status) {
        return new WorkflowRule(type, source, target, conclusion, status);
    }

    @BeforeEach
    void setUp() {
        workflow = new Workflow("Test Workflow", "WF-001", "1.0", "Test doc", "TYPE", "SUBTYPE");

        ActivityNode a1 = activity("ST", "AC1", "Activity 1");
        ActivityNode a2 = activity("ST", "AC2", "Activity 2");
        ActivityNode a3 = activity("ST", "AC3", "Activity 3");

        Conclusion conclusion = new Conclusion("APR", "Approved");
        a1.addConclusion(conclusion);

        workflow.addActivity(a1);
        workflow.addActivity(a2);
        workflow.addActivity(a3);

        workflow.addStage(new Stage("Stage A", "ST"));
        workflow.addInconsistency(new Inconsistency(10, "Missing stage"));

        workflow.addRule(rule(RuleType.START_TO_TASK, null, a1, "NV"));
        workflow.addRule(ruleWithConclusion(RuleType.SPLIT_TO_TASK, a1, a2, conclusion, null));
        workflow.addRule(rule(RuleType.TASK_TO_END, a2, null, "CLOSED"));
        workflow.addRule(rule(RuleType.TASK_TO_TASK, a2, a3, null));

        engine = new WorkflowEngineImpl(workflow);
    }

    @Test
    @DisplayName("constructor throws NullPointerException when workflow is null")
    void constructor_nullWorkflow_throws() {
        assertThrows(NullPointerException.class, () -> new WorkflowEngineImpl(null));
    }

    @Test
    @DisplayName("getWorkflow returns the wrapped workflow")
    void getWorkflow_returnsWorkflow() {
        assertSame(workflow, engine.getWorkflow());
    }

    @Nested
    @DisplayName("validate and isValid")
    class ValidateTests {

        @Test
        @DisplayName("validate returns inconsistencies")
        void validate_returnsInconsistencies() {
            List<Inconsistency> issues = engine.validate();
            assertEquals(1, issues.size());
            assertEquals(10, issues.get(0).getType());
        }

        @Test
        @DisplayName("isValid returns false when there are inconsistencies")
        void isValid_false_whenInconsistencies() {
            assertFalse(engine.isValid());
        }

        @Test
        @DisplayName("isValid returns true when no inconsistencies")
        void isValid_true_whenNoInconsistencies() {
            Workflow clean = new Workflow("Clean", "ID", "1.0", "doc", "T", "ST");
            clean.addActivity(activity("ST", "AC1", "Activity 1"));
            assertTrue(new WorkflowEngineImpl(clean).isValid());
        }
    }

    @Nested
    @DisplayName("listActivities")
    class ListActivitiesTests {

        @Test
        @DisplayName("returns all activities")
        void listActivities_returnsAll() {
            assertEquals(3, engine.listActivities().size());
        }

        @Test
        @DisplayName("returned list is unmodifiable")
        void listActivities_isUnmodifiable() {
            assertThrows(UnsupportedOperationException.class,
                    () -> engine.listActivities().clear());
        }
    }

    @Test
    @DisplayName("listStages returns all stages")
    void listStages_returnsAll() {
        assertEquals(1, engine.listStages().size());
        assertEquals("ST", engine.listStages().get(0).getCode());
    }

    @Test
    @DisplayName("listRules returns all rules")
    void listRules_returnsAll() {
        assertEquals(4, engine.listRules().size());
    }

    @Nested
    @DisplayName("findActivity")
    class FindActivityTests {

        @Test
        @DisplayName("finds existing activity by abbreviation")
        void findActivity_found() {
            ActivityNode found = engine.findActivity("ST-AC1");
            assertEquals("Activity 1", found.getName());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown abbreviation")
        void findActivity_notFound_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> engine.findActivity("XX-YY"));
            assertTrue(ex.getMessage().contains("XX-YY"));
        }
    }

    @Nested
    @DisplayName("nextSteps")
    class NextStepsTests {

        @Test
        @DisplayName("returns outgoing transitions from ST-AC1")
        void nextSteps_returnsTransitions() {
            List<NextStep> steps = engine.nextSteps("ST-AC1");
            assertEquals(1, steps.size());
            assertEquals("ST-AC2", steps.get(0).targetActivity().getAbbreviation());
            assertEquals("APR", steps.get(0).conclusion());
            assertEquals("SPLIT_TO_TASK", steps.get(0).ruleType());
        }

        @Test
        @DisplayName("returns multiple transitions from ST-AC2")
        void nextSteps_multipleTransitions() {
            List<NextStep> steps = engine.nextSteps("ST-AC2");
            assertEquals(2, steps.size());
        }

        @Test
        @DisplayName("returns empty list for terminal activity")
        void nextSteps_terminalActivity_returnsEmpty() {
            assertTrue(engine.nextSteps("ST-AC3").isEmpty());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown activity")
        void nextSteps_unknownActivity_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> engine.nextSteps("XX-YY"));
        }

        @Test
        @DisplayName("does not include START_TO_TASK rules as outgoing")
        void nextSteps_excludesEntryRules() {
            engine.nextSteps("ST-AC1").forEach(s ->
                    assertNotEquals("START_TO_TASK", s.ruleType()));
        }
    }

    @Nested
    @DisplayName("rulesTriggeredBy")
    class RulesTriggeredByTests {

        @Test
        @DisplayName("returns rules matching the given process status")
        void rulesTriggeredBy_matchingStatus() {
            List<WorkflowRule> rules = engine.rulesTriggeredBy("NV");
            assertEquals(1, rules.size());
            assertEquals(RuleType.START_TO_TASK, rules.get(0).getType());
        }

        @Test
        @DisplayName("returns empty list when no rules match")
        void rulesTriggeredBy_noMatch_returnsEmpty() {
            assertTrue(engine.rulesTriggeredBy("UNKNOWN").isEmpty());
        }

        @Test
        @DisplayName("throws for null processStatus")
        void rulesTriggeredBy_null_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> engine.rulesTriggeredBy(null));
        }

        @Test
        @DisplayName("throws for blank processStatus")
        void rulesTriggeredBy_blank_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> engine.rulesTriggeredBy("   "));
        }
    }
}