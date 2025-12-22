package com.eventui.common.dto;

/**
 * DTO específico para progreso de misiones.
 * Usado cuando solo necesitamos información de progreso sin todos los detalles.
 */
public record MissionProgressDTO(
        String missionId,
        int current,
        int target,
        float percentage
) {

    /**
     * Constructor que calcula automáticamente el porcentaje.
     */
    public MissionProgressDTO(String missionId, int current, int target) {
        this(missionId, current, target, calculatePercentage(current, target));
    }

    private static float calculatePercentage(int current, int target) {
        if (target == 0) return 0.0f;
        return Math.min(1.0f, (float) current / target);
    }
}
