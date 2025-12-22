package com.eventui.common.contract.persistence;

import java.util.Map;
import java.util.UUID;

/**
 * Eventos que indican que datos deben persistirse.
 * El Core emite estos eventos y el Plugin de Persistencia escucha.
 */
public sealed interface PersistenceEvent permits
        PersistenceEvent.MissionProgressChanged,
        PersistenceEvent.MissionCompleted,
        PersistenceEvent.PlayerDataChanged {

    /**
     * @return ID del jugador afectado
     */
    UUID playerId();

    /**
     * @return Timestamp del evento
     */
    long timestamp();

    /**
     * @return true si este evento debe guardarse inmediatamente (crítico)
     */
    boolean isCritical();

    /**
     * Progreso de una misión cambió.
     */
    record MissionProgressChanged(
            UUID playerId,
            String missionId,
            int progress,
            long timestamp,
            boolean isCritical
    ) implements PersistenceEvent {

        public MissionProgressChanged(UUID playerId, String missionId, int progress) {
            this(playerId, missionId, progress, System.currentTimeMillis(), false);
        }
    }

    /**
     * Una misión se completó.
     * Este evento es CRÍTICO y debe guardarse inmediatamente.
     */
    record MissionCompleted(
            UUID playerId,
            String missionId,
            long completionTime,
            long timestamp,
            boolean isCritical
    ) implements PersistenceEvent {

        public MissionCompleted(UUID playerId, String missionId, long completionTime) {
            this(playerId, missionId, completionTime, System.currentTimeMillis(), true);
        }

        @Override
        public boolean isCritical() {
            return true; // Siempre crítico
        }
    }

    /**
     * Datos generales del jugador cambiaron.
     */
    record PlayerDataChanged(
            UUID playerId,
            Map<String, Object> delta,
            long timestamp,
            boolean isCritical
    ) implements PersistenceEvent {

        public PlayerDataChanged(UUID playerId, Map<String, Object> delta) {
            this(playerId, delta, System.currentTimeMillis(), false);
        }
    }
}