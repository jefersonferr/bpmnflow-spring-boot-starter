package org.bpmnflow.starter.api;

import org.bpmnflow.WorkflowEngine;
import org.bpmnflow.WorkflowEngine.NextStep;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Inconsistency;
import org.bpmnflow.model.RuleType;
import org.bpmnflow.model.Stage;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.model.WorkflowRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WorkflowApiController")
@ExtendWith(MockitoExtension.class)
class WorkflowApiControllerTest {

    @Mock
    private WorkflowEngine engine;

    @InjectMocks
    private WorkflowApiController controller;

    private Workflow workflow;
    private ActivityNode activity;

    @BeforeEach
    void setUp() {
        workflow = new Workflow("My Process", "PROC-001", "1.0",
                "Process documentation", "RD", "ODN");
        activity = new ActivityNode("TR", "TR1", "Triage", null);
    }

    @Nested
    @DisplayName("GET /bpmnflow/info")
    class InfoTests {

        @Test
        @DisplayName("returns 200 with workflow metadata")
        void info_returns200WithMetadata() {
            when(engine.getWorkflow()).thenReturn(workflow);
            when(engine.isValid()).thenReturn(true);
            when(engine.listActivities()).thenReturn(List.of(activity));
            when(engine.listStages()).thenReturn(List.of(new Stage("Triage", "TR")));
            when(engine.listRules()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.info();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("My Process", body.get("name"));
            assertEquals("PROC-001",   body.get("id"));
            assertEquals("1.0",        body.get("version"));
            assertEquals("RD",         body.get("process_type"));
            assertEquals("ODN",        body.get("process_subtype"));
            assertEquals(true,         body.get("valid"));
            assertEquals(1,            body.get("activityCount"));
            assertEquals(1,            body.get("stageCount"));
            assertEquals(0,            body.get("ruleCount"));
        }

        @Test
        @DisplayName("returns empty strings for null workflow fields")
        void info_nullFields_returnsEmptyStrings() {
            Workflow nullWorkflow = new Workflow(null, null, null, null, null, null);
            when(engine.getWorkflow()).thenReturn(nullWorkflow);
            when(engine.isValid()).thenReturn(false);
            when(engine.listActivities()).thenReturn(List.of());
            when(engine.listStages()).thenReturn(List.of());
            when(engine.listRules()).thenReturn(List.of());

            Map<String, Object> body = controller.info().getBody();
            assertNotNull(body);
            assertEquals("", body.get("name"));
            assertEquals("", body.get("process_type"));
        }
    }

    @Nested
    @DisplayName("GET /bpmnflow/validate")
    class ValidateTests {

        @Test
        @DisplayName("returns valid=true when no inconsistencies")
        void validate_noInconsistencies() {
            when(engine.validate()).thenReturn(List.of());

            Map<String, Object> body = controller.validate().getBody();
            assertNotNull(body);
            assertEquals(true, body.get("valid"));
        }

        @Test
        @DisplayName("returns valid=false when inconsistencies exist")
        void validate_withInconsistencies() {
            when(engine.validate()).thenReturn(List.of(new Inconsistency(10, "Missing stage")));

            Map<String, Object> body = controller.validate().getBody();
            assertNotNull(body);
            assertEquals(false, body.get("valid"));
            List<?> inconsistencies = (List<?>) body.get("inconsistencies");
            assertNotNull(inconsistencies);
            assertEquals(1, inconsistencies.size());
        }
    }

    @Nested
    @DisplayName("GET /bpmnflow/activities")
    class ActivitiesTests {

        @Test
        @DisplayName("returns 200 with list of activities")
        void activities_returns200() {
            when(engine.listActivities()).thenReturn(List.of(activity));

            ResponseEntity<List<ActivityNode>> response = controller.activities();
            assertEquals(HttpStatus.OK, response.getStatusCode());
            List<ActivityNode> body = response.getBody();
            assertNotNull(body);
            assertEquals(1, body.size());
        }
    }

    @Nested
    @DisplayName("GET /bpmnflow/activities/{abbreviation}")
    class ActivityByAbbreviationTests {

        @Test
        @DisplayName("returns 200 when activity found")
        void activity_found_returns200() {
            when(engine.findActivity("TR-TR1")).thenReturn(activity);

            ResponseEntity<?> response = controller.activity("TR-TR1");
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(activity, response.getBody());
        }

        @Test
        @DisplayName("returns 404 when activity not found")
        void activity_notFound_returns404() {
            when(engine.findActivity("XX-YY"))
                    .thenThrow(new IllegalArgumentException("Not found"));

            assertEquals(HttpStatus.NOT_FOUND, controller.activity("XX-YY").getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /bpmnflow/activities/{abbreviation}/next")
    class NextStepsTests {

        @Test
        @DisplayName("returns 200 with next steps")
        void nextSteps_returns200() {
            NextStep step = new NextStep(activity, "APR", "IN_PROGRESS", "SPLIT_TO_TASK");
            when(engine.nextSteps("TR-TR1")).thenReturn(List.of(step));

            ResponseEntity<?> response = controller.nextSteps("TR-TR1");
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertNotNull(body);
            assertEquals("TR-TR1", body.get("activity"));
            List<?> nextSteps = (List<?>) body.get("nextSteps");
            assertNotNull(nextSteps);
            assertEquals(1, nextSteps.size());
        }

        @Test
        @DisplayName("returns 404 when activity not found")
        void nextSteps_notFound_returns404() {
            when(engine.nextSteps("XX-YY"))
                    .thenThrow(new IllegalArgumentException("Not found"));

            assertEquals(HttpStatus.NOT_FOUND, controller.nextSteps("XX-YY").getStatusCode());
        }
    }

    @Test
    @DisplayName("GET /bpmnflow/stages returns 200")
    void stages_returns200() {
        when(engine.listStages()).thenReturn(List.of(new Stage("Triage", "TR")));

        ResponseEntity<List<Stage>> response = controller.stages();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Stage> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
    }

    @Test
    @DisplayName("GET /bpmnflow/rules returns 200")
    void rules_returns200() {
        when(engine.listRules()).thenReturn(List.of());

        ResponseEntity<List<WorkflowRule>> response = controller.rules();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<WorkflowRule> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isEmpty());
    }

    @Nested
    @DisplayName("GET /bpmnflow/rules/by-status")
    class RulesByStatusTests {

        @Test
        @DisplayName("returns 200 with matching rules")
        void rulesByStatus_returns200() {
            WorkflowRule rule = new WorkflowRule(RuleType.START_TO_TASK, null, activity, null, "NV");
            when(engine.rulesTriggeredBy("NV")).thenReturn(List.of(rule));

            ResponseEntity<?> response = controller.rulesByStatus("NV");
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertNotNull(body);
            assertEquals("NV", body.get("processStatus"));
            List<?> rules = (List<?>) body.get("rules");
            assertNotNull(rules);
            assertEquals(1, rules.size());
        }

        @Test
        @DisplayName("returns 400 for invalid status")
        void rulesByStatus_invalid_returns400() {
            when(engine.rulesTriggeredBy(""))
                    .thenThrow(new IllegalArgumentException("processStatus must not be blank"));
            ResponseEntity<?> response = controller.rulesByStatus("");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertNotNull(body);
            assertTrue(body.containsKey("error"));
        }
    }
}