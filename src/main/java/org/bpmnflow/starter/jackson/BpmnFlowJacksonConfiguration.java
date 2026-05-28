package org.bpmnflow.starter.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.ApiActivityNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers Jackson MixIns directly on the shared {@link ObjectMapper} bean
 * after all beans are initialized, so that {@link ActivityNode} subclasses
 * are serialised with a {@code nodeType} discriminator field.
 *
 * <p>Uses {@link PostConstruct} to guarantee the MixIns are applied after
 * the {@link ObjectMapper} is fully configured by Spring Boot — avoiding
 * ordering issues with {@code Jackson2ObjectMapperBuilderCustomizer} when
 * SpringDoc or other libraries also customize the mapper.</p>
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

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void registerMixIns() {
        objectMapper.addMixIn(ActivityNode.class,    ActivityNodeMixin.class);
        objectMapper.addMixIn(ApiActivityNode.class, ApiActivityNodeMixin.class);
    }
}