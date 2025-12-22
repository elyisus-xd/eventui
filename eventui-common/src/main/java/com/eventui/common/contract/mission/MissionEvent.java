package com.eventui.common.contract.mission;

import com.eventui.common.model.MissionState;
import java.util.UUID;

/**
 * Eventos emitidos por el Core cuando el estado de misiones cambia.
 * El ViewModel se suscribe a estos eventos para actualizar la UI.*
 * Los eventos son inmutables y se emiten DESPUÉS de que el cambio ocurrió.
 */
public sealed interface MissionEvent permits
        MissionEvent.ProgressChanged,
        MissionEvent.StateChanged,
        MissionEvent.Unlocked,
        MissionEvent.Failed {

    /**
     * @return ID del jugador afectado
     */
    UUID playerId();

    /**
     * @return ID de la misión afectada
     */
    String missionId();

    /**
     * @return Timestamp del evento (epoch millis)
     */
    long timestamp();

    /**
     * Emitido cuando el progreso de una misión cambia.
     */
    record ProgressChanged(
            UUID playerId,
            String missionId,
            int oldValue,
            int newValue,
            int target,
            long timestamp
    ) implements MissionEvent {

        public ProgressChanged(UUID playerId, String missionId, int oldValue, int newValue, int target) {
            this(playerId, missionId, oldValue, newValue, target, System.currentTimeMillis());
        }

        /**
         * @return Progreso como porcentaje (0.0 a 1.0)
         */
        public float getProgressPercentage() {
            if (target == 0) return 0.0f;
            return (float) newValue / target;
        }
    }

    /**
     * Emitido cuando el estado de una misión cambia.
     */
    record StateChanged(
            UUID playerId,
            String missionId,
            MissionState oldState,
            MissionState newState,
            long timestamp
    ) implements MissionEvent {

        public StateChanged(UUID playerId, String missionId, MissionState oldState, MissionState newState) {
            this(playerId, missionId, oldState, newState, System.currentTimeMillis());
        }
    }

    /**
     * Emitido cuando una misión se desbloquea (LOCKED → AVAILABLE).
     */
    record Unlocked(
            UUID playerId,
            String missionId,
            long timestamp
    ) implements MissionEvent {

        public Unlocked(UUID playerId, String missionId) {
            this(playerId, missionId, System.currentTimeMillis());
        }
    }

    /**
     * Emitido cuando una misión falla.
     */
    record Failed(
            UUID playerId,
            String missionId,
            String reason,
            long timestamp
    ) implements MissionEvent {

        public Failed(UUID playerId, String missionId, String reason) {
            this(playerId, missionId, reason, System.currentTimeMillis());
        }
    }
}
