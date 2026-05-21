package org.bpmnflow.starter.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bpmnflow.model.ActivityNode;
import org.bpmnflow.model.ApiActivityNode;

/**
 * Jackson MixIn that adds polymorphic type information to {@link ActivityNode}
 * without modifying {@code bpmnflow-core} (which carries no Jackson dependency).
 *
 * <p>Registered via {@link BpmnFlowJacksonConfiguration}. The discriminator
 * field {@code nodeType} is included in every serialized activity:</p>
 * <ul>
 *   <li>{@code "nodeType": "TASK"}     — plain {@link ActivityNode}</li>
 *   <li>{@code "nodeType": "API_TASK"} — {@link ApiActivityNode} (includes {@code apiHandler})</li>
 * </ul>
 */
@JsonTypeInfo(
        use      = JsonTypeInfo.Id.NAME,
        include  = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "nodeType",
        visible  = true,
        defaultImpl = ActivityNode.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ActivityNode.class,    name = "TASK"),
        @JsonSubTypes.Type(value = ApiActivityNode.class, name = "API_TASK")
})
public abstract class ActivityNodeMixin {
    // No members — annotations only
}