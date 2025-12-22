package com.eventui.common.contract.persistence;

import com.eventui.common.dto.Result;

/**
 * Validador de estado cargado desde persistencia.
 * El Core implementa esta interfaz para validar datos antes de usarlos.
 * Previene corrupción de datos o exploits.
 */
public interface StateValidator {

    /**
     * Valida el estado cargado.
     *
     * @param state Estado a validar
     * @return Result con el estado validado, o Failure con errores
     */
    Result<PlayerMissionState> validate(PlayerMissionState state);

    /**
     * Valida que una misión existe y es válida.
     *
     * @param missionId ID de la misión
     * @return true si la misión existe en las definiciones
     */
    boolean isMissionValid(String missionId);

    /**
     * Sanitiza datos potencialmente corruptos.
     *
     * @param state Estado potencialmente corrupto
     * @return Estado sanitizado (elimina misiones inválidas, corrige progreso, etc.)
     */
    PlayerMissionState sanitize(PlayerMissionState state);
}
