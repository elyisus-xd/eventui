package com.eventui.common.contract.mission;

import com.eventui.common.dto.MissionDTO;
import com.eventui.common.dto.MissionProgressDTO;
import com.eventui.common.model.MissionState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz para consultas de solo lectura de misiones.
 * El ViewModel usa esta interfaz para obtener datos del Core.
 *
 * Todas las operaciones son síncronas y no modifican estado.
 */
public interface MissionQuery {

    /**
     * Obtiene todas las misiones de un jugador en un estado específico.
     *
     * @param playerId ID del jugador
     * @param state Estado a filtrar
     * @return Lista inmutable de misiones en ese estado
     */
    List<MissionDTO> getByState(UUID playerId, MissionState state);

    /**
     * Obtiene una misión por su ID.
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return Optional con la misión, o empty si no existe
     */
    Optional<MissionDTO> getById(UUID playerId, String missionId);

    /**
     * Obtiene el progreso actual de una misión.
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return DTO con el progreso, o empty si no existe o no está activa
     */
    Optional<MissionProgressDTO> getProgress(UUID playerId, String missionId);

    /**
     * Obtiene todas las misiones desbloqueadas (disponibles) para un jugador.
     *
     * @param playerId ID del jugador
     * @return Lista de misiones disponibles para aceptar
     */
    List<MissionDTO> getAvailableMissions(UUID playerId);

    /**
     * Obtiene todas las misiones activas de un jugador.
     *
     * @param playerId ID del jugador
     * @return Lista de misiones actualmente en progreso
     */
    List<MissionDTO> getActiveMissions(UUID playerId);

    /**
     * Obtiene todas las misiones completadas de un jugador.
     *
     * @param playerId ID del jugador
     * @return Lista de misiones completadas
     */
    List<MissionDTO> getCompletedMissions(UUID playerId);

    /**
     * Verifica si un jugador puede activar una misión.
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return true si cumple todos los prerequisites
     */
    boolean canActivate(UUID playerId, String missionId);

    /**
     * Obtiene misiones por categoría.
     *
     * @param playerId ID del jugador
     * @param category Categoría (ej: "combat", "exploration")
     * @return Lista de misiones en esa categoría
     */
    List<MissionDTO> getByCategory(UUID playerId, String category);
}
