package org.bpmnflow;

import lombok.Getter;
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
 * and local development environments without any path manipulation.</p>
 *
 * <p>{@link #getConfigStream()} exposes the config as a fresh {@link InputStream} on each call,
 * allowing the {@code WorkflowApiController} to re-parse an uploaded model using the same
 * validation rules that were active at startup.</p>
 */
public class WorkflowLoader {

    @Getter
    private final Workflow workflow;
    private final String configPath;            // internal — not exposed
    private final ResourceLoader resourceLoader; // internal — not exposed

    public WorkflowLoader(String modelPath, String configPath, ResourceLoader resourceLoader) {
        this.configPath     = configPath;
        this.resourceLoader = resourceLoader;

        try (InputStream modelStream  = resolve(modelPath);
             InputStream configStream = resolve(configPath)) {

            var config = ConfigLoader.loadConfig(configStream);
            this.workflow = ModelParser.parser(modelStream, config);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load BPMNFlow model from '" + modelPath +
                            "' with config '" + configPath + "'", e
            );
        }
    }

    /**
     * Opens and returns a fresh {@link InputStream} over the YAML config file.
     *
     * <p>The caller is responsible for closing the stream. Intended for use by
     * {@code WorkflowApiController} when re-parsing an uploaded BPMN model so the
     * same validation rules apply.</p>
     *
     * @return a new open stream over the config resource
     * @throws IllegalStateException if the config resource cannot be opened
     */
    public InputStream getConfigStream() {
        try {
            return resolve(configPath);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to re-open BPMNFlow config from '" + configPath + "'", e
            );
        }
    }

    private InputStream resolve(String path) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalArgumentException("BPMNFlow resource not found: " + path);
        }
        return resource.getInputStream();
    }
}