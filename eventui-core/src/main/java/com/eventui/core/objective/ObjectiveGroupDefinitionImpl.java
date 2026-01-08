// eventui-core/src/main/java/com/eventui/core/objective/ObjectiveGroupDefinitionImpl.java

package com.eventui.core.objective;

import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveGroupDefinition;
import com.eventui.api.objective.ObjectiveGroupType;

import java.util.Collections;
import java.util.List;

public record ObjectiveGroupDefinitionImpl(
        String id,
        ObjectiveGroupType type,
        String description,
        List<ObjectiveDefinition> objectives,
        int weight,
        List<ObjectiveGroupDefinition> nestedGroups
) implements ObjectiveGroupDefinition {

    public ObjectiveGroupDefinitionImpl {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Group ID cannot be null");
        if (type == null) throw new IllegalArgumentException("Group type cannot be null");

        objectives = objectives != null ? Collections.unmodifiableList(List.copyOf(objectives)) : List.of();
        nestedGroups = nestedGroups != null ? Collections.unmodifiableList(List.copyOf(nestedGroups)) : List.of();
    }

    @Override
    public String getId() { return id; }

    @Override
    public ObjectiveGroupType getType() { return type; }

    @Override
    public String getDescription() { return description; }

    @Override
    public List<ObjectiveDefinition> getObjectives() { return objectives; }

    @Override
    public int getWeight() { return weight; }

    @Override
    public List<ObjectiveGroupDefinition> getNestedGroups() { return nestedGroups; }
}
