package com.eventui.common.contract.mission;

import com.eventui.common.dto.Result;
import java.util.UUID;

/**
 * Interfaz para comandos de escritura sobre misiones.
 * El ViewModel usa esta interfaz para modificar el estado de misiones.*
 * Todas las operaciones pueden fallar y retornan Result.
 */
public interface MissionCommand {

    /**
     * Activa una misión (cambia de AVAILABLE a ACTIVE).
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return Success si se activó correctamente, Failure con razón si falló
     */
    Result<Void> activateMission(UUID playerId, String missionId);

    /**
     * Abandona una misión activa (cambia de ACTIVE a AVAILABLE).
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return Success si se abandonó correctamente, Failure con razón si falló
     */
    Result<Void> abandonMission(UUID playerId, String missionId);

    /**
     * Completa manualmente una misión (admin/debug).
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return Success si se completó, Failure si falló
     */
    Result<Void> completeMission(UUID playerId, String missionId);

    /**
     * Resetea una misión para que pueda repetirse.
     * Solo funciona si la misión es repeatable.
     *
     * @param playerId ID del jugador
     * @param missionId ID de la misión
     * @return Success si se reseteó, Failure si no es repetible o falló
     */
    Result<Void> resetMission(UUID playerId, String missionId);
}
