package com.eventui.common.contract.persistence;

import com.eventui.common.dto.Result;
import java.util.UUID;

/**
 * Interfaz para cargar y guardar estado de misiones.
 * El Plugin implementa esta interfaz para interactuar con el almacenamiento.
 */
public interface MissionStateLoader {

    /**
     * Carga el estado de misiones de un jugador.
     *
     * @param playerId ID del jugador
     * @return Result con el estado cargado, o Failure si no existe o falló
     */
    Result<PlayerMissionState> loadPlayer(UUID playerId);

    /**
     * Guarda el estado de misiones de un jugador.
     *
     * @param state Estado a guardar
     * @return Success si se guardó correctamente, Failure si falló
     */
    Result<Void> savePlayer(PlayerMissionState state);

    /**
     * Verifica si existe estado guardado para un jugador.
     *
     * @param playerId ID del jugador
     * @return true si existe estado guardado
     */
    boolean hasData(UUID playerId);

    /**
     * Elimina el estado de un jugador (admin/debug).
     *
     * @param playerId ID del jugador
     * @return Success si se eliminó, Failure si falló
     */
    Result<Void> deletePlayer(UUID playerId);
}