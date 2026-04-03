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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring Boot auto-configuration for BPMNFlow.
 *
 * <p>Registers:
 * <ol>
 *   <li>{@link WorkflowLoader} — parses the BPMN model and config at startup</li>
 *   <li>{@link AtomicReference}&lt;{@link WorkflowEngine}&gt; — the shared mutable engine holder</li>
 *   <li>{@link WorkflowEngine} — delegates to the AtomicReference for backward compatibility</li>
 *   <li>{@link WorkflowApiController} — REST endpoints (only in web apps, only when expose-api=true)</li>
 * </ol>
 *
 * <p>The {@code AtomicReference<WorkflowEngine>} is the single source of truth for the active engine.
 * Both {@link WorkflowApiController} and any application bean that injects it will always see the
 * most recent model — including models uploaded at runtime via {@code POST /bpmnflow/model}.</p>
 *
 * <p>All beans are conditional: if the application already defines an
 * {@code AtomicReference<WorkflowEngine>}, auto-configuration backs off completely.</p>
 */
@SuppressWarnings("unused")
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
     * The shared mutable engine holder. Both {@link WorkflowApiController} and application beans
     * that inject this reference will always resolve to the currently active engine.
     *
     * <p>When a new model is uploaded via {@code POST /bpmnflow/model}, the controller calls
     * {@code engineRef.set(...)} on this very instance — so all other beans sharing the reference
     * automatically reflect the new model on the next call to {@code engineRef.get()}.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public AtomicReference<WorkflowEngine> workflowEngineRef(WorkflowLoader loader) {
        return new AtomicReference<>(new WorkflowEngineImpl(loader.getWorkflow()));
    }

    /**
     * Exposes a {@link WorkflowEngine} bean for backward compatibility with beans that inject
     * {@code WorkflowEngine} directly instead of {@code AtomicReference<WorkflowEngine>}.
     *
     * <p><strong>Note:</strong> beans that inject {@code WorkflowEngine} directly will NOT see
     * model updates from {@code POST /bpmnflow/model}. Inject
     * {@code AtomicReference<WorkflowEngine>} and call {@code .get()} to always get the current
     * model.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowEngine workflowEngine(AtomicReference<WorkflowEngine> engineRef) {
        return engineRef.get();
    }

    /**
     * Registers the REST API only in web applications and when {@code bpmnflow.expose-api=true}.
     */
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnMissingBean(WorkflowApiController.class)
    @ConditionalOnProperty(prefix = "bpmnflow", name = "expose-api", havingValue = "true", matchIfMissing = true)
    public WorkflowApiController workflowApiController(AtomicReference<WorkflowEngine> engineRef,
                                                       WorkflowLoader loader) {
        return new WorkflowApiController(engineRef, loader);
    }
}