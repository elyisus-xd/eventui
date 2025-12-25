package com.eventui.fabric.client.viewmodel;

import com.eventui.api.bridge.MessageType;
import com.eventui.api.event.EventState;
import com.eventui.fabric.client.bridge.ClientEventBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ViewModel que conecta la UI con el ClientEventBridge.
 */
public class EventViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventViewModel.class);

    private final UUID playerId;
    private final ClientEventBridge bridge;

    // Caché local
    private final Map<String, EventData> eventsCache;

    // Listeners para cambios
    private final List<Consumer<List<EventData>>> changeListeners;

    public EventViewModel(UUID playerId) {
        this.playerId = playerId;
        this.bridge = ClientEventBridge.getInstance();
        this.eventsCache = new ConcurrentHashMap<>();
        this.changeListeners = new ArrayList<>();

        subscribeToUpdates();
    }

    /**
     * Se suscribe a actualizaciones del bridge.
     */
    private void subscribeToUpdates() {
        LOGGER.info("Subscribing to bridge updates...");

// Escuchar actualizaciones de progreso
        bridge.registerMessageHandler(
                MessageType.PROGRESS_UPDATE,
                message -> {
                    String eventId = message.getPayload().get("event_id");
                    String objectiveId = message.getPayload().get("objective_id");
                    String currentStr = message.getPayload().get("current");
                    String targetStr = message.getPayload().get("target");
                    String description = message.getPayload().get("description"); // ← NUEVO

                    if (currentStr != null && targetStr != null) {
                        int current = Integer.parseInt(currentStr);
                        int target = Integer.parseInt(targetStr);

                        LOGGER.info("Received progress update: event={}, objective={}, progress={}/{}, desc={}",
                                eventId, objectiveId, current, target, description);

                        updateProgressInCache(eventId, objectiveId, current, target, description); // ← PASAR descripción
                        notifyListeners();
                    }
                }
        );


        LOGGER.info("Bridge subscriptions registered");
    }

    /**
     * Actualiza el progreso en el caché local.
     */
    /**
     * Actualiza el progreso en el caché local.
     */
    private void updateProgressInCache(String eventId, String objectiveId, int current, int target, String objectiveDescription) {
        EventData event = eventsCache.get(eventId);

        if (event != null) {
            event.currentProgress = current;
            event.targetProgress = target;
            event.currentObjectiveDescription = objectiveDescription != null ? objectiveDescription : "In progress...";
            LOGGER.info("Updated cache for event {}: {}/{} - {}", eventId, current, target, objectiveDescription);
        } else {
            LOGGER.info("Event {} not in cache, creating new entry", eventId);
            EventData newEvent = new EventData(
                    eventId,
                    "Event: " + eventId,
                    "Loading...",
                    EventState.IN_PROGRESS,
                    current,
                    target,
                    objectiveDescription != null ? objectiveDescription : "Loading..."
            );
            eventsCache.put(eventId, newEvent);
        }
    }



    /**
     * Actualiza el estado en el caché.
     */
    private void updateStateInCache(String eventId, EventState newState) {
        EventData event = eventsCache.get(eventId);
        if (event != null) {
            event.state = newState;
        }
    }

    /**
     * Solicita la lista de eventos al servidor.
     */
    public void requestEvents() {
        LOGGER.info("Requesting events from server...");

        EventData mockEvent = new EventData(
                "tutorial_mining",
                "§6Tutorial de Minería",
                "Aprende a minar tus primeros recursos",
                EventState.IN_PROGRESS,
                0,
                10,
                "Mina 10 bloques de piedra" // ← AGREGAR descripción inicial
        );

        eventsCache.put(mockEvent.id, mockEvent);
        notifyListeners();

        LOGGER.info("Initial event loaded: {}", mockEvent.id);
    }


    /**
     * Obtiene todos los eventos.
     */
    public List<EventData> getAllEvents() {
        return new ArrayList<>(eventsCache.values());
    }

    /**
     * Registra un listener para cambios.
     */
    public void addChangeListener(Consumer<List<EventData>> listener) {
        changeListeners.add(listener);
        LOGGER.info("Change listener added, total listeners: {}", changeListeners.size());
    }
    /**
     * Remueve un listener específico.
     */
    public void removeChangeListener(Consumer<List<EventData>> listener) {
        changeListeners.remove(listener);
        LOGGER.info("Change listener removed, total listeners: {}", changeListeners.size());
    }

    /**
     * Notifica a todos los listeners.
     */
    private void notifyListeners() {
        List<EventData> events = getAllEvents();
        LOGGER.info("Notifying {} listeners with {} events", changeListeners.size(), events.size());

        for (Consumer<List<EventData>> listener : changeListeners) {
            try {
                listener.accept(events);
            } catch (Exception e) {
                LOGGER.error("Error notifying listener", e);
            }
        }
    }

    /**
     * Limpia recursos.
     */
    public void dispose() {
        changeListeners.clear();
        eventsCache.clear();
        LOGGER.info("ViewModel disposed");
    }

    /**
     * Clase de datos para eventos (simplificada para UI).
     */
    public static class EventData {
        public final String id;
        public final String displayName;
        public final String description;
        public EventState state;
        public int currentProgress;
        public int targetProgress;
        public String currentObjectiveDescription; // ← NUEVO

        public EventData(String id, String displayName, String description,
                         EventState state, int currentProgress, int targetProgress,
                         String currentObjectiveDescription) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.state = state;
            this.currentProgress = currentProgress;
            this.targetProgress = targetProgress;
            this.currentObjectiveDescription = currentObjectiveDescription;
        }

        public float getProgressPercentage() {
            if (targetProgress == 0) return 0f;
            return (float) currentProgress / targetProgress;
        }
    }
}
