package com.eventui.core.v2.event;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.objective.ObjectiveDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementación inmutable de EventDefinition.
 * ARQUITECTURA:
 * - Record para inmutabilidad
 * - Se crea desde JSON por EventConfigLoader
 * - NO contiene lógica de negocio
 */
public record EventDefinitionImpl(
        String id,
        String displayName,
        String description,
        List<ObjectiveDefinition> objectives,
        Map<String, String> uiResources,
        Map<String, String> metadata
) implements EventDefinition {

    // Constructor compacto para validación
    public EventDefinitionImpl {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Event ID cannot be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Event display name cannot be null or blank");
        }

        // Hacer colecciones inmutables
        objectives = objectives != null
                ? Collections.unmodifiableList(List.copyOf(objectives))
                : List.of();
        uiResources = uiResources != null
                ? Collections.unmodifiableMap(Map.copyOf(uiResources))
                : Map.of();
        metadata = metadata != null
                ? Collections.unmodifiableMap(Map.copyOf(metadata))
                : Map.of();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<ObjectiveDefinition> getObjectives() {
        return objectives;
    }

    @Override
    public Map<String, String> getUIResources() {
        return uiResources;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }
}
