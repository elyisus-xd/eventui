package com.eventui.common.model;

/**
 * Estados posibles de una misión.*
 * Transiciones válidas:
 * LOCKED → AVAILABLE (cuando se cumplen prerequisites)
 * AVAILABLE → ACTIVE (cuando el jugador acepta)
 * ACTIVE → COMPLETED (cuando se cumplen conditions)
 * ACTIVE → FAILED (timeout o condición de fallo)
 */
public enum MissionState {
    /**
     * Misión bloqueada por prerequisites no cumplidos.
     */
    LOCKED,

    /**
     * Misión disponible para ser aceptada.
     */
    AVAILABLE,

    /**
     * Misión aceptada y en progreso.
     */
    ACTIVE,

    /**
     * Misión completada exitosamente.
     */
    COMPLETED,

    /**
     * Misión fallada (timeout, muerte, etc.)
     */
    FAILED;

    /**
     * @return true si el estado permite iniciar la misión
     */
    public boolean canActivate() {
        return this == AVAILABLE;
    }

    /**
     * @return true si el estado permite abandonar la misión
     */
    public boolean canAbandon() {
        return this == ACTIVE;
    }

    /**
     * @return true si la misión está en un estado terminal
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
