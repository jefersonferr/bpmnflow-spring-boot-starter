package org.bpmnflow.starter.autoconfigure;

import org.bpmnflow.WorkflowEngine;
import org.bpmnflow.WorkflowEngineImpl;
import org.bpmnflow.WorkflowLoader;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.Inconsistency;
import org.bpmnflow.model.Stage;
import org.bpmnflow.model.Workflow;
import org.bpmnflow.model.WorkflowRule;
import org.bpmnflow.starter.api.WorkflowApiController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BpmnFlowAutoConfiguration")
class BpmnFlowAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BpmnFlowAutoConfiguration.class));

    @Test
    @DisplayName("registers WorkflowLoader bean with classpath paths")
    void registersWorkflowLoaderBean() {
        contextRunner
                .withPropertyValues(
                        "bpmnflow.model-path=classpath:models/model_01.bpmn",
                        "bpmnflow.config-path=classpath:config/test_config_01.yaml"
                )
                .run(context -> assertThat(context).hasSingleBean(WorkflowLoader.class));
    }

    @Test
    @DisplayName("registers WorkflowEngine bean of type WorkflowEngineImpl")
    void registersWorkflowEngineBean() {
        contextRunner
                .withPropertyValues(
                        "bpmnflow.model-path=classpath:models/model_01.bpmn",
                        "bpmnflow.config-path=classpath:config/test_config_01.yaml"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(WorkflowEngine.class);
                    assertThat(context.getBean(WorkflowEngine.class))
                            .isInstanceOf(WorkflowEngineImpl.class);
                });
    }

    @Test
    @DisplayName("registers WorkflowApiController when expose-api=true")
    void registersApiController_whenExposeApiTrue() {
        contextRunner
                .withPropertyValues(
                        "bpmnflow.model-path=classpath:models/model_01.bpmn",
                        "bpmnflow.config-path=classpath:config/test_config_01.yaml",
                        "bpmnflow.expose-api=true"
                )
                .run(context -> assertThat(context).hasSingleBean(WorkflowApiController.class));
    }

    @Test
    @DisplayName("does not register WorkflowApiController when expose-api=false")
    void doesNotRegisterApiController_whenExposeApiFalse() {
        contextRunner
                .withPropertyValues(
                        "bpmnflow.model-path=classpath:models/model_01.bpmn",
                        "bpmnflow.config-path=classpath:config/test_config_01.yaml",
                        "bpmnflow.expose-api=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(WorkflowApiController.class));
    }

    @Test
    @DisplayName("backs off WorkflowEngine when custom bean is present")
    void backsOff_whenCustomWorkflowEnginePresent() {
        contextRunner
                .withPropertyValues(
                        "bpmnflow.model-path=classpath:models/model_01.bpmn",
                        "bpmnflow.config-path=classpath:config/test_config_01.yaml"
                )
                .withUserConfiguration(CustomEngineConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(WorkflowEngine.class);
                    assertThat(context.getBean(WorkflowEngine.class))
                            .isInstanceOf(CustomEngineConfig.CustomEngine.class);
                });
    }

    @Test
    @DisplayName("BpmnFlowProperties has correct default values")
    void propertiesDefaults() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BpmnFlowAutoConfiguration.class))
                .withUserConfiguration(MockedLoaderConfig.class)
                .run(context -> {
                    BpmnFlowProperties props = context.getBean(BpmnFlowProperties.class);
                    assertThat(props.getModelPath()).isEqualTo("classpath:process.bpmn");
                    assertThat(props.getConfigPath()).isEqualTo("classpath:bpmn-config.yaml");
                    assertThat(props.isExposeApi()).isTrue();
                });
    }

    @Test
    @DisplayName("BpmnFlowProperties binds custom values from application properties")
    void propertiesBinding() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BpmnFlowAutoConfiguration.class))
                .withUserConfiguration(MockedLoaderConfig.class)
                .withPropertyValues(
                        "bpmnflow.model-path=classpath:custom.bpmn",
                        "bpmnflow.config-path=classpath:custom-config.yaml",
                        "bpmnflow.expose-api=false"
                )
                .run(context -> {
                    BpmnFlowProperties props = context.getBean(BpmnFlowProperties.class);
                    assertThat(props.getModelPath()).isEqualTo("classpath:custom.bpmn");
                    assertThat(props.getConfigPath()).isEqualTo("classpath:custom-config.yaml");
                    assertThat(props.isExposeApi()).isFalse();
                });
    }

    @Configuration
    static class MockedLoaderConfig {

        @Bean
        public WorkflowLoader workflowLoader() {
            return new WorkflowLoader(
                    "classpath:models/model_01.bpmn",
                    "classpath:config/test_config_01.yaml",
                    new DefaultResourceLoader()
            );
        }
    }

    @Configuration
    static class CustomEngineConfig {

        static class CustomEngine implements WorkflowEngine {
            @Override public Workflow getWorkflow() { return null; }
            @Override public List<Inconsistency> validate() { return List.of(); }
            @Override public boolean isValid() { return true; }
            @Override public List<ActivityNode> listActivities() { return List.of(); }
            @Override public List<Stage> listStages() { return List.of(); }
            @Override public List<WorkflowRule> listRules() { return List.of(); }
            @Override public List<WorkflowEngine.NextStep> nextSteps(String a) { return List.of(); }
            @Override public List<WorkflowRule> rulesTriggeredBy(String s) { return List.of(); }
            @Override public ActivityNode findActivity(String a) { return null; }
        }

        @Bean
        public WorkflowEngine workflowEngine() { return new CustomEngine(); }
    }
}