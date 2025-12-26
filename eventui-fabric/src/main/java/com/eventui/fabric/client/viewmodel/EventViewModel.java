package com.eventui.fabric.client.viewmodel;

import com.eventui.api.bridge.MessageType;
import com.eventui.api.event.EventState;
import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ViewModel que conecta la UI con el ClientEventBridge.
 * FASE 2: Procesa múltiples eventos del servidor.
 */
public class EventViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventViewModel.class);

    private final UUID playerId;
    private final ClientEventBridge bridge;

    // Caché local
    private final Map<String, EventData> eventsCache;

    // Listeners para cambios
    private final List<Consumer<List<EventData>>> changeListeners;

    private final Gson gson = new Gson();

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
        bridge.registerMessageHandler(MessageType.PROGRESS_UPDATE, message -> {
            String eventId = message.getPayload().get("event_id");
            String objectiveId = message.getPayload().get("objective_id");
            String currentStr = message.getPayload().get("current");
            String targetStr = message.getPayload().get("target");
            String description = message.getPayload().get("description");

            if (currentStr != null && targetStr != null) {
                int current = Integer.parseInt(currentStr);
                int target = Integer.parseInt(targetStr);

                LOGGER.info("Received progress update: event={}, objective={}, progress={}/{}, desc={}",
                        eventId, objectiveId, current, target, description);

                updateProgressInCache(eventId, objectiveId, current, target, description);
                notifyListeners();
            }
        });

// Escuchar cambios de estado
        bridge.registerMessageHandler(MessageType.EVENT_STATE_CHANGED, message -> {
            String eventId = message.getPayload().get("event_id");
            String newStateStr = message.getPayload().get("new_state");

            if (eventId != null && newStateStr != null) {
                EventState newState = EventState.valueOf(newStateStr);

                LOGGER.info("Received state change: event={}, newState={}", eventId, newState);

                updateStateInCache(eventId, newState);

                // ✅ NUEVO: Si es COMPLETED, solicitar datos frescos del servidor
                if (newState == EventState.COMPLETED || newState == EventState.AVAILABLE) {
                    LOGGER.info("Requesting fresh data after state change to {}", newState);
                    requestEvents(); // Recargar todos los eventos
                } else {
                    notifyListeners();
                }
            }
        });

        // Escuchar respuestas de EVENT_DATA
        bridge.registerMessageHandler(MessageType.EVENT_DATA_RESPONSE, message -> {
            LOGGER.info("Received EVENT_DATA_RESPONSE");
            handleEventDataResponse(message);
        });

        LOGGER.info("Bridge subscriptions registered");
    }

    /**
     * Actualiza el estado en el caché.
     */
    private void updateStateInCache(String eventId, EventState newState) {
        EventData event = eventsCache.get(eventId);

        if (event != null) {
            event.state = newState;
            LOGGER.info("Updated state for event '{}': {}", eventId, newState);
        } else {
            LOGGER.warn("Cannot update state, event not in cache: {}", eventId);
        }
    }


    /**
     * NUEVO: Procesa la respuesta con TODOS los eventos del servidor.
     */
    private void handleEventDataResponse(com.eventui.api.bridge.BridgeMessage message) {
        try {
            String eventsJson = message.getPayload().get("events");
            String countStr = message.getPayload().get("count");

            LOGGER.info("Processing EVENT_DATA_RESPONSE with {} events", countStr);

            if (eventsJson == null || eventsJson.isEmpty()) {
                LOGGER.warn("Empty events JSON in response");
                return;
            }

            // Deserializar lista de eventos
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> eventsList = gson.fromJson(eventsJson, listType);

            LOGGER.info("Deserialized {} events from JSON", eventsList.size());

            // Limpiar caché anterior
            eventsCache.clear();

            // Procesar cada evento
            for (Map<String, Object> eventMap : eventsList) {
                String id = (String) eventMap.get("id");
                String displayName = (String) eventMap.get("displayName");
                String description = (String) eventMap.get("description");
                String stateStr = (String) eventMap.get("state");

                EventState state = EventState.valueOf(stateStr);

                // Progreso
                int currentProgress = 0;
                int targetProgress = 0;
                String currentObjective = null;

                if (eventMap.containsKey("currentProgress")) {
                    currentProgress = ((Number) eventMap.get("currentProgress")).intValue();
                    targetProgress = ((Number) eventMap.get("targetProgress")).intValue();
                    currentObjective = (String) eventMap.get("currentObjective");
                }

                EventData eventData = new EventData(
                        id,
                        displayName,
                        description,
                        state,
                        currentProgress,
                        targetProgress,
                        currentObjective
                );

                eventsCache.put(id, eventData);

                LOGGER.info("  Loaded event: {} - {} (state={})", id, displayName, state);
            }

            LOGGER.info("✓ Successfully loaded {} events into cache", eventsCache.size());

            // Notificar a la UI
            notifyListeners();

        } catch (Exception e) {
            LOGGER.error("Failed to process EVENT_DATA_RESPONSE", e);
        }
    }

    /**
     * Actualiza el progreso en el caché local.
     */
    private void updateProgressInCache(String eventId, String objectiveId, int current, int target, String objectiveDescription) {
        EventData event = eventsCache.get(eventId);

        if (event != null) {
            // Actualizar progreso existente
            event.state = EventState.IN_PROGRESS;
            event.currentProgress = current;
            event.targetProgress = target;
            event.currentObjectiveDescription = objectiveDescription != null ? objectiveDescription : "In progress...";

            LOGGER.info("Updated cache for event '{}': {}/{}, desc={}", eventId, current, target, objectiveDescription);
        } else {
            LOGGER.info("Event not in cache, creating new entry: {}", eventId);

            EventData newEvent = new EventData(
                    eventId,
                    "Event " + eventId,
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
     * Solicita la lista de eventos al servidor.
     */
    public void requestEvents() {
        LOGGER.info("Requesting events from server via bridge...");

        try {
            // Enviar solicitud al servidor sin especificar event_id
            com.eventui.fabric.client.bridge.BridgeMessageImpl request =
                    new com.eventui.fabric.client.bridge.BridgeMessageImpl(
                            MessageType.REQUEST_EVENT_DATA,
                            Map.of("player_id", playerId.toString()),
                            playerId
                    );

            bridge.sendMessage(request);

            LOGGER.info("✓ Event request sent to server");

        } catch (Exception e) {
            LOGGER.error("Failed to request events", e);
        }
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
        public String currentObjectiveDescription;

        public EventData(String id, String displayName, String description, EventState state,
                         int currentProgress, int targetProgress, String currentObjectiveDescription) {
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
