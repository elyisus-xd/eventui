package com.eventui.common.contract.persistence;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado serializable de las misiones de un jugador.
 * El Core expone este estado para que el Plugin lo persista.
 * Esta interfaz abstrae el formato de persistencia (JSON, NBT, SQL, etc.)
 */
public interface PlayerMissionState {

    /**
     * @return ID del jugador
     */
    UUID getPlayerId();

    /**
     * @return Mapa de missionId → progreso actual
     */
    Map<String, Integer> getProgress();

    /**
     * @return Set de misiones completadas (IDs)
     */
    Set<String> getCompletedMissions();

    /**
     * @return Set de misiones activas (IDs)
     */
    Set<String> getActiveMissions();

    /**
     * @return Mapa de missionId → timestamp de inicio
     */
    Map<String, Long> getStartTimestamps();

    /**
     * @return Mapa de missionId → timestamp de completado
     */
    Map<String, Long> getCompletionTimestamps();

    /**
     * @return Versión del esquema de datos (para migraciones)
     */
    int getSchemaVersion();
}