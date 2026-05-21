package org.bpmnflow.starter.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson MixIn for {@link org.bpmnflow.model.ApiActivityNode}.
 *
 * <p>Overrides the {@code nodeType} property to always return {@code "API_TASK"},
 * so that API consumers can distinguish API-handler service tasks from plain
 * tasks in the {@code GET /process/activities} response.</p>
 */
public abstract class ApiActivityNodeMixin {

    @JsonProperty("nodeType")
    public abstract String getNodeType();
}