package com.eventui.core.storage;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.api.event.EventState;
import com.eventui.core.EventUIPlugin;
import com.eventui.core.event.EventProgressImpl;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Almacenamiento en memoria de eventos y progreso.*
 * - TODO en memoria (se pierde al reiniciar)
 * - Sin persistencia a disco/base de datos
 * - Thread-safe para servidores*
 */
public class EventStorage {

    private static final Logger LOGGER = Logger.getLogger(EventStorage.class.getName());

    // Definiciones de eventos
    private final Map<String, EventDefinition> eventDefinitions;

    private final EventUIPlugin plugin;

    // Progreso de jugadores: playerId → (eventId → progress)
    private final Map<UUID, Map<String, EventProgressImpl>> playerProgress;

    public EventStorage(EventUIPlugin plugin) {  // ✅ Modificar constructor
        this.eventDefinitions = new ConcurrentHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
        this.plugin = plugin;  // ✅ Guardar referencia
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
// ✅ NUEVO: Registrar evento como activo cuando está IN_PROGRESS
            if (progress.getState() == EventState.IN_PROGRESS) {
                // Obtener ObjectiveTracker desde plugin
                plugin.getObjectiveTracker().registerActiveEvent(playerId, eventId);
            }
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

    /**
     * Obtiene todos los progresos de todos los jugadores.
     * Usado para inicializar el índice de eventos activos.
     */
    public Map<UUID, Map<String, EventProgressImpl>> getAllProgress() {
        return Collections.unmodifiableMap(playerProgress);
    }
}
