package com.eventui.core.player;

import com.eventui.common.model.MissionState;
import com.eventui.core.mission.Mission;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado de misiones de un jugador específico.
 * Cada jugador tiene su propia instancia de esta clase.
 */
public class PlayerMissionData {
    private final UUID playerId;

    // missionId -> instancia de misión con estado
    private final Map<String, Mission> missions;

    // missionId -> timestamp de inicio
    private final Map<String, Long> startTimestamps;

    // missionId -> timestamp de completado
    private final Map<String, Long> completionTimestamps;

    public PlayerMissionData(UUID playerId) {
        this.playerId = playerId;
        this.missions = new ConcurrentHashMap<>();
        this.startTimestamps = new ConcurrentHashMap<>();
        this.completionTimestamps = new ConcurrentHashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * Inicializa una misión para este jugador (copia de la definición).
     */
    public void initMission(Mission template) {
        if (!missions.containsKey(template.getId())) {
            missions.put(template.getId(), template.copy());
        }
    }

    /**
     * Obtiene una misión del jugador.
     */
    public Optional<Mission> getMission(String missionId) {
        return Optional.ofNullable(missions.get(missionId));
    }

    /**
     * Obtiene todas las misiones del jugador en un estado específico.
     */
    public List<Mission> getMissionsByState(MissionState state) {
        return missions.values().stream()
                .filter(m -> m.getState() == state)
                .toList();
    }

    /**
     * Cambia el estado de una misión.
     */
    public void setMissionState(String missionId, MissionState state) {
        Mission mission = missions.get(missionId);
        if (mission != null) {
            mission.setState(state);

            // Registrar timestamps
            if (state == MissionState.ACTIVE) {
                startTimestamps.put(missionId, System.currentTimeMillis());
            } else if (state == MissionState.COMPLETED) {
                completionTimestamps.put(missionId, System.currentTimeMillis());
            }
        }
    }

    /**
     * @return Set de IDs de misiones completadas
     */
    public Set<String> getCompletedMissionIds() {
        return missions.values().stream()
                .filter(m -> m.getState() == MissionState.COMPLETED)
                .map(Mission::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * @return Set de IDs de misiones activas
     */
    public Set<String> getActiveMissionIds() {
        return missions.values().stream()
                .filter(m -> m.getState() == MissionState.ACTIVE)
                .map(Mission::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Obtiene el timestamp de cuando se inició una misión.
     */
    public Optional<Long> getStartTimestamp(String missionId) {
        return Optional.ofNullable(startTimestamps.get(missionId));
    }

    /**
     * Obtiene el timestamp de cuando se completó una misión.
     */
    public Optional<Long> getCompletionTimestamp(String missionId) {
        return Optional.ofNullable(completionTimestamps.get(missionId));
    }

    /**
     * @return Todas las misiones del jugador
     */
    public Collection<Mission> getAllMissions() {
        return List.copyOf(missions.values());
    }
}
