package org.bpmnflow.starter.api;

import org.bpmnflow.WorkflowEngine;
import org.bpmnflow.WorkflowEngine.NextStep;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Inconsistency;
import org.bpmnflow.model.Stage;
import org.bpmnflow.model.WorkflowRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Workflow", description = "Inspection and navigation of the parsed BPMN model")
@RestController
@RequestMapping("/bpmnflow")
public class WorkflowApiController {

    private final WorkflowEngine engine;

    public WorkflowApiController(WorkflowEngine engine) {
        this.engine = engine;
    }

    @Operation(summary = "Workflow metadata",
            description = "Returns name, id, version, type, subtype, documentation and health summary")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        var wf = engine.getWorkflow();
        return ResponseEntity.ok(Map.of(
                "name",           orEmpty(wf.getName()),
                "id",             orEmpty(wf.getId()),
                "version",        orEmpty(wf.getVersion()),
                "process_type",    orEmpty(wf.getType()),
                "process_subtype", orEmpty(wf.getSubtype()),
                "documentation",  orEmpty(wf.getDocumentation()),
                "valid",          engine.isValid(),
                "activityCount",  engine.listActivities().size(),
                "stageCount",     engine.listStages().size(),
                "ruleCount",      engine.listRules().size()
        ));
    }

    @Operation(summary = "Validate BPMN model",
            description = "Returns all inconsistencies detected during parsing. Empty list means the model is valid.")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        List<Inconsistency> issues = engine.validate();
        return ResponseEntity.ok(Map.of(
                "valid",           issues.isEmpty(),
                "inconsistencies", issues
        ));
    }

    @Operation(summary = "List all activities",
            description = "Returns every activity parsed from the BPMN model")
    @GetMapping("/activities")
    public ResponseEntity<List<ActivityNode>> activities() {
        return ResponseEntity.ok(engine.listActivities());
    }

    @Operation(summary = "Find activity by abbreviation",
            description = "Returns a single activity by its abbreviation (stageCode-activityCode), e.g. TR-TR1")
    @GetMapping("/activities/{abbreviation}")
    public ResponseEntity<?> activity(
            @Parameter(description = "Activity abbreviation", example = "TR-TR1")
            @PathVariable("abbreviation") String abbreviation) {
        try {
            return ResponseEntity.ok(engine.findActivity(abbreviation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Next steps from an activity",
            description = "Returns every possible transition reachable from the given activity.")
    @GetMapping("/activities/{abbreviation}/next")
    public ResponseEntity<?> nextSteps(
            @Parameter(description = "Activity abbreviation", example = "TR-TR1")
            @PathVariable("abbreviation") String abbreviation) {
        try {
            List<NextStep> steps = engine.nextSteps(abbreviation);
            return ResponseEntity.ok(Map.of(
                    "activity",  abbreviation,
                    "nextSteps", steps
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "List all stages",
            description = "Returns all stages declared in the workflow lanes")
    @GetMapping("/stages")
    public ResponseEntity<List<Stage>> stages() {
        return ResponseEntity.ok(engine.listStages());
    }

    @Operation(summary = "List all rules",
            description = "Returns all workflow rules (transitions between activities)")
    @GetMapping("/rules")
    public ResponseEntity<List<WorkflowRule>> rules() {
        return ResponseEntity.ok(engine.listRules());
    }

    @Operation(summary = "Rules triggered by process status",
            description = "Returns all rules whose process status matches the given value. Example: status=NV")
    @GetMapping("/rules/by-status")
    public ResponseEntity<?> rulesByStatus(
            @Parameter(description = "Process status value", example = "NV")
            @RequestParam("status") String status) {
        try {
            List<WorkflowRule> rules = engine.rulesTriggeredBy(status);
            return ResponseEntity.ok(Map.of(
                    "processStatus", status,
                    "rules",         rules
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }
}