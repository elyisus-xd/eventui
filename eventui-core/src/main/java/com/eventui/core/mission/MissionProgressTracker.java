package com.eventui.core.mission;

import com.eventui.common.contract.signal.GameSignal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rastrea el progreso de objetivos de misiones.
 */
public class MissionProgressTracker {
    // playerId -> missionId -> objectiveId -> progress
    private final Map<UUID, Map<String, Map<String, Integer>>> progressData;

    public MissionProgressTracker() {
        this.progressData = new HashMap<>();
    }

    /**
     * Inicializa el progreso de una misión para un jugador.
     */
    public void initMission(UUID playerId, Mission mission) {
        progressData.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(mission.getId(), new HashMap<>());

        // Inicializar todos los objetivos en 0
        for (MissionObjective objective : mission.getObjectives()) {
            setProgress(playerId, mission.getId(), objective.getId(), 0);
        }
    }

    /**
     * Procesa una señal del juego y actualiza progreso si aplica.
     *
     * @return Cantidad incrementada (0 si no aplica)
     */
    public int processSignal(UUID playerId, Mission mission, GameSignal signal) {
        MissionObjective primaryObjective = mission.getPrimaryObjective();
        if (primaryObjective == null) return 0;

        if (!primaryObjective.matches(signal)) return 0;

        // Incrementar progreso
        int increment = getIncrementFromSignal(signal);
        int currentProgress = getProgress(playerId, mission.getId(), primaryObjective.getId());
        int newProgress = Math.min(currentProgress + increment, primaryObjective.getCount());

        setProgress(playerId, mission.getId(), primaryObjective.getId(), newProgress);

        return increment;
    }

    /**
     * Obtiene el progreso actual de un objetivo.
     */
    public int getProgress(UUID playerId, String missionId, String objectiveId) {
        return progressData.getOrDefault(playerId, Map.of())
                .getOrDefault(missionId, Map.of())
                .getOrDefault(objectiveId, 0);
    }

    /**
     * Establece el progreso de un objetivo.
     */
    public void setProgress(UUID playerId, String missionId, String objectiveId, int progress) {
        progressData.computeIfAbsent(playerId, k -> new HashMap<>())
                .computeIfAbsent(missionId, k -> new HashMap<>())
                .put(objectiveId, progress);
    }

    /**
     * Verifica si una misión está completada (todos sus objetivos).
     */
    public boolean isCompleted(UUID playerId, Mission mission) {
        for (MissionObjective objective : mission.getObjectives()) {
            int progress = getProgress(playerId, mission.getId(), objective.getId());
            if (progress < objective.getCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resetea el progreso de una misión.
     */
    public void resetMission(UUID playerId, String missionId) {
        Map<String, Map<String, Integer>> playerData = progressData.get(playerId);
        if (playerData != null) {
            playerData.remove(missionId);
        }
    }

    /**
     * Obtiene el incremento de progreso según el tipo de señal.
     */
    private int getIncrementFromSignal(GameSignal signal) {
        return switch (signal) {
            case GameSignal.EntityKilled killed -> 1;
            case GameSignal.ItemCrafted crafted -> crafted.count();
            case GameSignal.BlockPlaced placed -> 1;
            case GameSignal.BlockBroken broken -> 1;
            case GameSignal.ItemPickedUp pickup -> pickup.count();
            default -> 0;
        };
    }
}
