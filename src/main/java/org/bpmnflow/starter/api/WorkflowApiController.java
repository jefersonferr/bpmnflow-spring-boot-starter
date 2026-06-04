package org.bpmnflow.starter.api;

import org.bpmnflow.WorkflowEngine;
import org.bpmnflow.WorkflowEngineImpl;
import org.bpmnflow.WorkflowLoader;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.ApiActivityNode;
import org.bpmnflow.model.Inconsistency;
import org.bpmnflow.model.Stage;
import org.bpmnflow.starter.dto.NextStepDto;
import org.bpmnflow.starter.dto.WorkflowRuleDto;
import org.bpmnflow.parser.ConfigLoader;
import org.bpmnflow.parser.ModelParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
@Tag(name = "Process", description = "Inspection and navigation of the parsed BPMN model")
@RestController
@RequestMapping("/process")
public class WorkflowApiController {

    private final AtomicReference<WorkflowEngine> engineRef;
    private final WorkflowLoader loader;

    public WorkflowApiController(AtomicReference<WorkflowEngine> engineRef, WorkflowLoader loader) {
        this.engineRef = engineRef;
        this.loader    = loader;
    }

    // ---------------------------------------------------------------
    // Model upload
    // ---------------------------------------------------------------

    @Operation(
            summary = "Upload a new BPMN model",
            description = "Parses the uploaded .bpmn file using the existing YAML config and replaces " +
                    "the active model for all subsequent requests. The change persists until the " +
                    "application is restarted or another model is uploaded."
    )
    @PostMapping(value = "/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadModel(
            @Parameter(description = "BPMN model file (.bpmn)", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Uploaded file is empty"));
        }

        try (InputStream modelStream  = file.getInputStream();
             InputStream configStream = loader.getConfigStream()) {

            var config   = ConfigLoader.loadConfig(configStream);
            var workflow = ModelParser.parser(modelStream, config);
            engineRef.set(new WorkflowEngineImpl(workflow));

            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "unknown";

            return ResponseEntity.ok(Map.of(
                    "message",        "Model replaced successfully",
                    "filename",        filename,
                    "valid",           engineRef.get().isValid(),
                    "activityCount",   workflow.getActivities().size(),
                    "inconsistencies", workflow.getInconsistencies()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to parse uploaded model: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // Workflow metadata
    // ---------------------------------------------------------------

    @Operation(summary = "Workflow metadata",
            description = "Returns name, id, version, type, subtype, documentation and health summary")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        WorkflowEngine engine = engineRef.get();
        var wf = engine.getWorkflow();
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("name",             orEmpty(wf.getName())),
                Map.entry("id",               orEmpty(wf.getId())),
                Map.entry("version",          orEmpty(wf.getVersion())),
                Map.entry("process_type",     orEmpty(wf.getType())),
                Map.entry("process_subtype",  orEmpty(wf.getSubtype())),
                Map.entry("documentation",    orEmpty(wf.getDocumentation())),
                Map.entry("valid",            engine.isValid()),
                Map.entry("activityCount",    engine.listActivities().size()),
                Map.entry("apiActivityCount", engine.listApiActivities().size()),
                Map.entry("stageCount",       engine.listStages().size()),
                Map.entry("ruleCount",        engine.listRules().size())
        ));
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    @Operation(summary = "Validate BPMN model",
            description = "Returns all inconsistencies detected during parsing. Empty list means the model is valid.")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        List<Inconsistency> issues = engineRef.get().validate();
        return ResponseEntity.ok(Map.of(
                "valid",           issues.isEmpty(),
                "inconsistencies", issues
        ));
    }

    // ---------------------------------------------------------------
    // Activities
    // ---------------------------------------------------------------

    @Operation(summary = "List all activities",
            description = "Returns every activity parsed from the BPMN model. " +
                    "ApiActivityNode entries include an 'apiHandler' field and carry nodeType='API_TASK'.")
    @GetMapping("/activities")
    public ResponseEntity<List<ActivityNode>> activities() {
        return ResponseEntity.ok(engineRef.get().listActivities());
    }

    @Operation(summary = "List API-handler activities",
            description = "Returns only ServiceTask activities that declare a connector or job definition. " +
                    "Each entry includes the full apiHandler definition (endpoint, method, mappings).")
    @GetMapping("/api-activities")
    public ResponseEntity<List<ApiActivityNode>> apiActivities() {
        return ResponseEntity.ok(engineRef.get().listApiActivities());
    }

    @Operation(summary = "Find activity by abbreviation",
            description = "Returns a single activity by its abbreviation as defined in the active BPMN model")
    @GetMapping("/activities/{abbreviation}")
    public ResponseEntity<?> activity(
            @Parameter(description = "Activity abbreviation as defined in the active BPMN model")
            @PathVariable("abbreviation") String abbreviation) {
        try {
            return ResponseEntity.ok(engineRef.get().findActivity(abbreviation));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Next steps from an activity",
            description = "Returns every possible transition reachable from the given activity. " +
                    "source and target are activity abbreviations.")
    @GetMapping("/activities/{abbreviation}/next")
    public ResponseEntity<?> nextSteps(
            @Parameter(description = "Activity abbreviation as defined in the active BPMN model")
            @PathVariable("abbreviation") String abbreviation) {
        try {
            List<NextStepDto> steps = engineRef.get().nextSteps(abbreviation)
                    .stream()
                    .map(NextStepDto::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "activity",  abbreviation,
                    "nextSteps", steps
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---------------------------------------------------------------
    // Stages
    // ---------------------------------------------------------------

    @Operation(summary = "List all stages",
            description = "Returns all stages declared in the workflow lanes")
    @GetMapping("/stages")
    public ResponseEntity<List<Stage>> stages() {
        return ResponseEntity.ok(engineRef.get().listStages());
    }

    // ---------------------------------------------------------------
    // Rules
    // ---------------------------------------------------------------

    @Operation(summary = "List all rules",
            description = "Returns all workflow rules. " +
                    "source and target are activity abbreviations, not full objects.")
    @GetMapping("/rules")
    public ResponseEntity<List<WorkflowRuleDto>> rules() {
        List<WorkflowRuleDto> dtos = engineRef.get().listRules()
                .stream()
                .map(WorkflowRuleDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Rules triggered by process status",
            description = "Returns all rules whose process status matches the given value. " +
                    "source and target are activity abbreviations.")
    @GetMapping("/rules/by-status")
    public ResponseEntity<?> rulesByStatus(
            @Parameter(description = "Process status value as defined in the active BPMN model")
            @RequestParam("status") String status) {
        try {
            List<WorkflowRuleDto> dtos = engineRef.get().rulesTriggeredBy(status)
                    .stream()
                    .map(WorkflowRuleDto::from)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "processStatus", status,
                    "rules",         dtos
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }
}