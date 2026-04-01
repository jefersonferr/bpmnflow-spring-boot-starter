package org.bpmnflow;

import org.bpmnflow.model.Workflow;
import org.bpmnflow.parser.ConfigLoader;
import org.bpmnflow.parser.ModelParser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

/**
 * Loads and parses a BPMN model and its associated YAML config at application startup.
 *
 * <p>Accepts both {@code classpath:} prefixed paths (resolved via Spring's {@link ResourceLoader})
 * and raw filesystem paths. This makes it work seamlessly inside JARs, containers,
 * and local development environments without any path manipulation.
 */
public class WorkflowLoader {

    private final Workflow workflow;

    public WorkflowLoader(String modelPath, String configPath, ResourceLoader resourceLoader) {
        try (InputStream modelStream = resolve(modelPath, resourceLoader);
             InputStream configStream = resolve(configPath, resourceLoader)) {

            var config = ConfigLoader.loadConfig(configStream);
            this.workflow = ModelParser.parser(modelStream, config);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load BPMNFlow model from '" + modelPath + "' with config '" + configPath + "'", e
            );
        }
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    private InputStream resolve(String path, ResourceLoader resourceLoader) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalArgumentException("BPMNFlow resource not found: " + path);
        }
        return resource.getInputStream();
    }
}
