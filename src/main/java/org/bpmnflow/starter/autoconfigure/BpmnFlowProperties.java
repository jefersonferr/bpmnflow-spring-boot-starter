package org.bpmnflow.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the BPMNFlow Spring Boot Starter.
 *
 * <p>All properties are declared under the {@code bpmnflow} prefix.
 * Example {@code application.yaml}:
 *
 * <pre>
 * bpmnflow:
 *   model-path: classpath:process.bpmn
 *   config-path: classpath:bpmn-config.yaml
 *   expose-api: true
 * </pre>
 */
@ConfigurationProperties(prefix = "bpmnflow")
public class BpmnFlowProperties {

    /**
     * Path to the BPMN model file.
     * Supports {@code classpath:} and absolute filesystem paths.
     */
    private String modelPath = "classpath:process.bpmn";

    /**
     * Path to the YAML validation/extraction config consumed by bpmn-model-parser.
     * Supports {@code classpath:} and absolute filesystem paths.
     */
    private String configPath = "classpath:bpmn-config.yaml";

    /**
     * When true, registers a REST controller at {@code /bpmnflow/**} exposing
     * workflow inspection and navigation endpoints. Default: true.
     */
    private boolean exposeApi = true;

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public String getConfigPath() { return configPath; }
    public void setConfigPath(String configPath) { this.configPath = configPath; }

    public boolean isExposeApi() { return exposeApi; }
    public void setExposeApi(boolean exposeApi) { this.exposeApi = exposeApi; }
}
