package com.eventui.common.dto;

import com.eventui.common.model.MissionState;
import java.util.List;

/**
 * DTO inmutable de una misión.
 * El Core expone SOLO este DTO, nunca sus entidades internas.
 */
public record MissionDTO(
        String id,
        String title,
        String description,
        MissionState state,
        int progress,
        int target,
        List<String> prerequisites,
        boolean isRepeatable,
        String category,
        String difficulty
) {

    /**
     * @return Progreso como porcentaje (0.0 a 1.0)
     */
    public float getProgressPercentage() {
        if (target == 0) return 0.0f;
        return (float) progress / target;
    }

    /**
     * @return true si la misión está completada
     */
    public boolean isCompleted() {
        return state == MissionState.COMPLETED;
    }

    /**
     * @return true si la misión está activa
     */
    public boolean isActive() {
        return state == MissionState.ACTIVE;
    }
}
