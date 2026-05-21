package org.bpmnflow.starter.jackson;

import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.ApiActivityNode;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Jackson MixIns via {@link Jackson2ObjectMapperBuilderCustomizer} so
 * that {@link ActivityNode} subclasses are serialised with a {@code nodeType}
 * discriminator field — without touching the {@code ObjectMapper} bean directly.
 *
 * <p>{@link Jackson2ObjectMapperBuilderCustomizer} is the Spring Boot idiomatic
 * way to contribute configuration to the shared {@code ObjectMapper}: it runs
 * during the builder phase, before the {@code ObjectMapper} is created, so
 * MixIns are always in place before the first serialization occurs.</p>
 *
 * <p>Result in {@code GET /process/activities}:</p>
 * <pre>{@code
 * [
 *   { "nodeType": "TASK",     "abbreviation": "SC-RCV", ... },
 *   { "nodeType": "API_TASK", "abbreviation": "SC-PMT_AUTH", ..., "apiHandler": { ... } }
 * ]
 * }</pre>
 */
@Configuration
public class BpmnFlowJacksonConfiguration {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bpmnFlowJacksonCustomizer() {
        return builder -> builder
                .mixIn(ActivityNode.class,    ActivityNodeMixin.class)
                .mixIn(ApiActivityNode.class, ApiActivityNodeMixin.class);
    }
}