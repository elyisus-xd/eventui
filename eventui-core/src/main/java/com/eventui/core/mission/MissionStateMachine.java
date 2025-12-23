package com.eventui.core.mission;

import com.eventui.common.model.MissionState;
import com.eventui.common.dto.Result;

/**
 * Máquina de estados para transiciones válidas de misiones.
 * Asegura que los cambios de estado sean válidos.
 */
public class MissionStateMachine {

    /**
     * Intenta cambiar el estado de una misión.
     *
     * @param currentState Estado actual
     * @param newState Estado deseado
     * @return Success si la transición es válida, Failure si no
     */
    public Result<MissionState> transition(MissionState currentState, MissionState newState) {
        if (!isValidTransition(currentState, newState)) {
            return new Result.Failure<>(
                    "Invalid state transition: " + currentState + " -> " + newState
            );
        }
        return new Result.Success<>(newState);
    }

    /**
     * Verifica si una transición es válida.
     */
    public boolean isValidTransition(MissionState from, MissionState to) {
        return switch (from) {
            case LOCKED -> to == MissionState.AVAILABLE;
            case AVAILABLE -> to == MissionState.ACTIVE;
            case ACTIVE -> to == MissionState.COMPLETED || to == MissionState.FAILED || to == MissionState.AVAILABLE;
            case COMPLETED -> to == MissionState.AVAILABLE; // si es repeatable
            case FAILED -> to == MissionState.AVAILABLE; // retry
        };
    }

    /**
     * @return true si el estado permite activar la misión
     */
    public boolean canActivate(MissionState state) {
        return state == MissionState.AVAILABLE;
    }

    /**
     * @return true si el estado permite abandonar la misión
     */
    public boolean canAbandon(MissionState state) {
        return state == MissionState.ACTIVE;
    }
}
