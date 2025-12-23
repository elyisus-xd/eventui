package com.eventui.core.loader;

import com.eventui.core.mission.Mission;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida definiciones de misiones cargadas.
 */
public class MissionValidator {

    /**
     * Valida una misión completa.
     */
    public ValidationResult validate(Mission mission) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validar ID
        if (mission.getId() == null || mission.getId().isBlank()) {
            errors.add("Mission ID cannot be empty");
        }

        // Validar título
        if (mission.getTitle() == null || mission.getTitle().isBlank()) {
            errors.add("Mission title cannot be empty");
        }

        // Validar que tenga al menos un objetivo
        if (mission.getObjectives().isEmpty()) {
            errors.add("Mission must have at least one objective");
        }

        // Advertir si no tiene descripción
        if (mission.getDescription() == null || mission.getDescription().isBlank()) {
            warnings.add("Mission has no description");
        }

        // Advertir si es repetible pero no tiene category
        if (mission.isRepeatable() && "general".equals(mission.getCategory())) {
            warnings.add("Repeatable mission should have a specific category");
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }
}
