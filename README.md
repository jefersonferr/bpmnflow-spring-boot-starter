# BPMNFlow Spring Boot Starter

> Zero-config Spring Boot integration for [bpmnflow-core](https://github.com/jefersonferr/bpmnflow-core) — parse a BPMN model at startup, query it at runtime, and optionally expose it as a REST API.

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Camunda 7](https://img.shields.io/badge/Camunda-7-blue)
![Camunda 8](https://img.shields.io/badge/Camunda-8-purple)
![CI](https://github.com/jefersonferr/bpmnflow-spring-boot-starter/actions/workflows/ci.yml/badge.svg)

---

## Table of Contents

- [What it does](#what-it-does)
- [Quick Start](#quick-start)
- [See it in action](#see-it-in-action)
- [Engine Support](#engine-support)
- [Configuration](#configuration)
- [WorkflowEngine API](#workflowengine-api)
- [REST API](#rest-api)
- [Customization](#customization)
- [What's Next](#whats-next)

---

## What it does

`bpmnflow-spring-boot-starter` wraps `bpmnflow-core` as a Spring Boot auto-configuration. Drop it into any Spring Boot application and you get:

- A **`WorkflowEngine`** bean ready for injection — no setup code required
- **BPMN model parsed at startup** from classpath or filesystem paths
- **Camunda 7 and Camunda 8 support** — declare the target engine in your `bpmn-config.yaml`
- **Hot-swap support** — upload a new `.bpmn` file at runtime via `POST /process/model` without restarting
- An optional **REST API** at `/process/**` for model inspection and navigation
- **Swagger UI** automatically available when springdoc is on the classpath
- Full **back-off support** — if you define your own `WorkflowEngine` bean, auto-configuration steps aside
- **Runnable demo available** — see [bpmnflow-spring-boot-demo](https://github.com/jefersonferr/bpmnflow-spring-boot-demo) for a complete working example

---

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>org.bpmnflow</groupId>
    <artifactId>bpmnflow-spring-boot-starter</artifactId>
    <version>3.2.0</version>
</dependency>
```

### 2. Add your files to `src/main/resources`

```
src/main/resources/
├── process.bpmn          ← your BPMN model (default path)
└── bpmn-config.yaml      ← your validation/extraction config (default path)
```

### 3. Inject and use

```java
@Service
public class CaseService {

    private final WorkflowEngine engine;

    public CaseService(WorkflowEngine engine) {
        this.engine = engine;
    }

    public List<WorkflowEngine.NextStep> getNextSteps(String activityAbbreviation) {
        return engine.nextSteps(activityAbbreviation);
    }

    public List<WorkflowRule> getEntryRules(String processStatus) {
        return engine.rulesTriggeredBy(processStatus);
    }
}
```

That's it. No `@Bean` methods, no `ModelParser` calls, no stream wiring.

### 4. Hot-swap the active model at runtime

```bash
curl -X POST http://localhost:8080/process/model \
  -F "file=@my-process.bpmn"
```

All subsequent requests to `/process/**` and any bean injecting `AtomicReference<WorkflowEngine>` will immediately reflect the new model.

---

## See it in action

The [bpmnflow-spring-boot-demo](https://github.com/jefersonferr/bpmnflow-spring-boot-demo) is a runnable Spring Boot application that demonstrates the starter end-to-end:

- A **Pizza Delivery** BPMN process pre-loaded at startup
- Generic `ProcessController` endpoints that navigate the model without any hardcoded business logic
- Hot-swap — upload a different `.bpmn` at runtime via `POST /process/model`
- Full Swagger UI at `http://localhost:8080/swagger-ui.html`

```bash
git clone https://github.com/jefersonferr/bpmnflow-spring-boot-demo.git
cd bpmnflow-spring-boot-demo
mvn spring-boot:run
```

---

## Engine Support

BPMNFlow supports BPMN models created with both **Camunda 7** and **Camunda 8** (Zeebe). The target engine is declared in your `bpmn-config.yaml` via the `engine` field — no code changes required.

### Camunda 7

```yaml
# bpmn-config.yaml
bpmn_model_parser:
  engine: camunda7   # default — can be omitted for backward compatibility
  model_properties:
    ...
```

Extension properties use the `camunda:` namespace in the BPMN XML:

```xml
<bpmn:task id="Task_1" name="My Task">
    <bpmn:extensionElements>
        <camunda:properties>
            <camunda:property name="stage"    value="ST" />
            <camunda:property name="activity" value="AC1" />
        </camunda:properties>
    </bpmn:extensionElements>
</bpmn:task>
```

### Camunda 8

```yaml
# bpmn-config.yaml
bpmn_model_parser:
  engine: camunda8
  model_properties:
    ...
```

Extension properties use the `zeebe:` namespace in the BPMN XML:

```xml
<bpmn:task id="Task_1" name="My Task">
    <bpmn:extensionElements>
        <zeebe:properties>
            <zeebe:property name="stage"    value="ST" />
            <zeebe:property name="activity" value="AC1" />
        </zeebe:properties>
    </bpmn:extensionElements>
</bpmn:task>
```

The `engine` field defaults to `camunda7` when omitted, ensuring full backward compatibility for existing configurations.

---

## Configuration

All properties are declared under the `bpmnflow` prefix. IDE auto-complete is supported via the included configuration metadata.

```yaml
bpmnflow:
  model-path: classpath:process.bpmn       # default
  config-path: classpath:bpmn-config.yaml  # default
  expose-api: true                         # default
```

| Property | Type | Default | Description |
|---|---|---|---|
| `bpmnflow.model-path` | `String` | `classpath:process.bpmn` | Path to the BPMN model. Supports `classpath:` and absolute filesystem paths. |
| `bpmnflow.config-path` | `String` | `classpath:bpmn-config.yaml` | Path to the YAML validation/extraction config. Supports `classpath:` and absolute filesystem paths. |
| `bpmnflow.expose-api` | `boolean` | `true` | When `true`, registers the REST controller at `/process/**`. |

The `engine` field (`camunda7` or `camunda8`) is declared inside `bpmn-config.yaml`, not in `application.yaml`. This keeps engine selection tied to the model config rather than the application config.

### Filesystem paths

Both `model-path` and `config-path` also accept raw filesystem paths, useful in containerized environments where files live outside the JAR:

```yaml
bpmnflow:
  model-path: /data/workflows/process.bpmn
  config-path: /data/workflows/bpmn-config.yaml
```

---

## WorkflowEngine API

`WorkflowEngine` is a read-only, thread-safe interface. All methods derive their results from the parsed model — there is no mutable state.

```java
// Full parsed model
Workflow workflow = engine.getWorkflow();

// Validation
boolean valid = engine.isValid();
List<Inconsistency> issues = engine.validate();

// Listing
List<ActivityNode> activities = engine.listActivities();
List<Stage>        stages     = engine.listStages();
List<WorkflowRule> rules      = engine.listRules();

// Navigation — abbreviation format is defined by the active BPMN model
ActivityNode activity = engine.findActivity(abbreviation);

// Next steps from a given activity
List<WorkflowEngine.NextStep> steps = engine.nextSteps(abbreviation);
// NextStep fields: targetActivity, conclusion, processStatus, ruleType

// Rules triggered by a given process status
List<WorkflowRule> entryRules = engine.rulesTriggeredBy(processStatus);
```

### `NextStep` record

`nextSteps(abbreviation)` returns a list of `NextStep` records, each describing one possible outgoing transition:

| Field | Type | Description |
|---|---|---|
| `targetActivity` | `ActivityNode` | The destination activity (`null` for end events) |
| `conclusion` | `String` | The conclusion code that triggers this path (may be `null`) |
| `processStatus` | `String` | The resulting process status after the transition (may be `null`) |
| `ruleType` | `String` | The rule type name, e.g. `TASK_TO_TASK`, `SPLIT_TO_TASK` |

### Live model awareness

Beans that need to always reflect the currently active model — including models uploaded at runtime — should inject `AtomicReference<WorkflowEngine>` instead of `WorkflowEngine` directly:

```java
@Component
public class ProcessNavigator {

    private final AtomicReference<WorkflowEngine> engineRef;

    public ProcessNavigator(AtomicReference<WorkflowEngine> engineRef) {
        this.engineRef = engineRef;
    }

    public List<WorkflowEngine.NextStep> getNextSteps(String abbreviation) {
        return engineRef.get().nextSteps(abbreviation);
    }
}
```

Beans that inject `WorkflowEngine` directly receive the engine active at startup and will not reflect subsequent model uploads.

---

## REST API

When `bpmnflow.expose-api=true` (the default) and the application is a web app, the following endpoints are registered at `/process/**`.

| Method | Path | Description |
|---|---|---|
| `POST` | `/process/model` | Upload a new `.bpmn` file to replace the active model at runtime |
| `GET` | `/process/info` | Process metadata: name, id, version, type, subtype, health summary |
| `GET` | `/process/validate` | Validation result and list of inconsistencies |
| `GET` | `/process/activities` | All activities in the workflow |
| `GET` | `/process/activities/{abbreviation}` | Single activity by abbreviation |
| `GET` | `/process/activities/{abbreviation}/next` | All outgoing transitions from a given activity |
| `GET` | `/process/stages` | All stages declared in the workflow lanes |
| `GET` | `/process/rules` | All workflow rules (transitions) |
| `GET` | `/process/rules/by-status?status={status}` | Rules whose process status matches the given value |

### Swagger UI

If `springdoc-openapi-starter-webmvc-ui` is on the classpath, the full OpenAPI documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

To enable Swagger UI, add the dependency to your project:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

The API is documented under the **Process** tag, with descriptions, parameter examples, and response codes for every endpoint.

### Disabling the API

```yaml
bpmnflow:
  expose-api: false
```

---

## Customization

### Custom `WorkflowEngine`

Define your own bean and auto-configuration backs off completely:

```java
@Configuration
public class MyEngineConfig {

    @Bean
    public WorkflowEngine workflowEngine(WorkflowLoader loader) {
        return new MyCustomEngine(loader.getWorkflow());
    }
}
```

`MyCustomEngine` must implement the `WorkflowEngine` interface. The `WorkflowLoader` bean is still registered and available for injection.

### Using `WorkflowLoader` directly

If you need access to the raw parsed `Workflow` object:

```java
@Component
public class ModelInspector {

    private final WorkflowLoader loader;

    public ModelInspector(WorkflowLoader loader) {
        this.loader = loader;
    }

    public Workflow getRawWorkflow() {
        return loader.getWorkflow();
    }
}
```

---

## What's Next

- Publication to Maven Central

---

## License

MIT — see [LICENSE](LICENSE).