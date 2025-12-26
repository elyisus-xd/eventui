package com.eventui.core.v2.storage;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.core.v2.event.EventProgressImpl;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Almacenamiento en memoria de eventos y progreso.*
 * ARQUITECTURA FASE 1:
 * - TODO en memoria (se pierde al reiniciar)
 * - Sin persistencia a disco/base de datos
 * - Thread-safe para servidores*
 * FASE 2+ agregará persistencia real.
 */
public class EventStorage {

    private static final Logger LOGGER = Logger.getLogger(EventStorage.class.getName());

    // Definiciones de eventos (cargadas del JSON)
    private final Map<String, EventDefinition> eventDefinitions;

    // Progreso de jugadores: playerId → (eventId → progress)
    private final Map<UUID, Map<String, EventProgressImpl>> playerProgress;

    public EventStorage() {
        this.eventDefinitions = new ConcurrentHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
    }

    // ========== Gestión de definiciones ==========

    /**
     * Registra una definición de evento.
     */
    public void registerEvent(EventDefinition definition) {
        eventDefinitions.put(definition.getId(), definition);
        LOGGER.info("Registered event definition: " + definition.getId());
    }

    /**
     * Registra múltiples eventos.
     */
    public void registerEvents(Map<String, EventDefinition> events) {
        eventDefinitions.putAll(events);
        LOGGER.info("Registered " + events.size() + " event definitions");
    }

    /**
     * Obtiene una definición de evento.
     */
    public Optional<EventDefinition> getEventDefinition(String eventId) {
        return Optional.ofNullable(eventDefinitions.get(eventId));
    }

    /**
     * Obtiene todas las definiciones.
     */
    public Map<String, EventDefinition> getAllEventDefinitions() {
        return Map.copyOf(eventDefinitions);
    }

    // ========== Gestión de progreso ==========

    /**
     * Obtiene o crea el progreso de un evento para un jugador.
     */
    public EventProgressImpl getOrCreateProgress(UUID playerId, String eventId) {
        EventDefinition definition = eventDefinitions.get(eventId);
        if (definition == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        Map<String, EventProgressImpl> playerEvents = playerProgress.computeIfAbsent(
                playerId,
                k -> new ConcurrentHashMap<>()
        );

        return playerEvents.computeIfAbsent(eventId, k -> {
            EventProgressImpl progress = new EventProgressImpl(
                    playerId,
                    eventId,
                    definition.getObjectives().stream()
                            .map(obj -> obj.getId())
                            .toList()
            );

            // Registrar objetivos con sus cantidades objetivo
            definition.getObjectives().forEach(obj ->
                    progress.registerObjective(obj.getId(), obj.getTargetAmount())
            );

            return progress;
        });
    }

    /**
     * Obtiene el progreso si existe.
     */
    public Optional<EventProgress> getProgress(UUID playerId, String eventId) {
        Map<String, EventProgressImpl> playerEvents = playerProgress.get(playerId);
        if (playerEvents == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerEvents.get(eventId));
    }

    /**
     * Limpia el progreso de un jugador (para testing o reset).
     */
    public void clearPlayerProgress(UUID playerId) {
        playerProgress.remove(playerId);
        LOGGER.info("Cleared progress for player: " + playerId);
    }

    /**
     * Elimina el progreso de un evento específico para un jugador.
     */
    public void removeProgress(UUID playerId, String eventId) {
        Map<String, EventProgressImpl> playerEvents = playerProgress.get(playerId);
        if (playerEvents != null) {
            playerEvents.remove(eventId);
            LOGGER.info("Removed progress for player " + playerId + ", event: " + eventId);
        }
    }

}
