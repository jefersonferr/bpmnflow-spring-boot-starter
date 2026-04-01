package org.bpmnflow.starter.autoconfigure;

import org.bpmnflow.starter.api.WorkflowApiController;
import org.bpmnflow.WorkflowEngine;
import org.bpmnflow.WorkflowEngineImpl;
import org.bpmnflow.WorkflowLoader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * Spring Boot auto-configuration for BPMNFlow.
 *
 * <p>Registers:
 * <ol>
 *   <li>{@link WorkflowLoader} — parses the BPMN model and config at startup</li>
 *   <li>{@link WorkflowEngine} — the navigation/validation service</li>
 *   <li>{@link WorkflowApiController} — REST endpoints (only in web apps, only when expose-api=true)</li>
 * </ol>
 *
 * <p>All beans are conditional: if the application already defines a {@link WorkflowEngine},
 * auto-configuration backs off completely.
 */
@AutoConfiguration
@EnableConfigurationProperties(BpmnFlowProperties.class)
public class BpmnFlowAutoConfiguration {

    /**
     * Loads the BPMN model and config files, producing a parsed {@link org.bpmnflow.model.Workflow}.
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowLoader workflowLoader(BpmnFlowProperties props, ResourceLoader resourceLoader) {
        return new WorkflowLoader(props.getModelPath(), props.getConfigPath(), resourceLoader);
    }

    /**
     * Creates the {@link WorkflowEngine} from the loaded workflow.
     * Override this bean in your application to provide a custom implementation.
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowEngine workflowEngine(WorkflowLoader loader) {
        return new WorkflowEngineImpl(loader.getWorkflow());
    }

    /**
     * Registers the REST API only in web applications and when {@code bpmnflow.expose-api=true}.
     */
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnMissingBean(WorkflowApiController.class)
    @ConditionalOnProperty(prefix = "bpmnflow", name = "expose-api", havingValue = "true", matchIfMissing = true)
    public WorkflowApiController workflowApiController(WorkflowEngine engine) {
        return new WorkflowApiController(engine);
    }
}
