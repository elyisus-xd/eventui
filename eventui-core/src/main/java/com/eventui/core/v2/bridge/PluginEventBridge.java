package com.eventui.core.v2.bridge;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.bridge.EventBridge;
import com.eventui.api.bridge.MessageType;
import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.api.event.EventState;
import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveProgress;
import com.eventui.api.ui.UIConfig;
import com.eventui.core.v2.EventUIPlugin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Implementación del EventBridge para el lado del PLUGIN (Paper).
 */
public class PluginEventBridge implements EventBridge {

    private static final Logger LOGGER = Logger.getLogger(PluginEventBridge.class.getName());

    private final EventUIPlugin plugin;
    private final Map<MessageType, MessageHandler> handlers;
    private final PluginNetworkHandler network;

    public PluginEventBridge(EventUIPlugin plugin) {
        this.plugin = plugin;
        this.handlers = new ConcurrentHashMap<>();
        this.network = new PluginNetworkHandler(this, plugin);

        registerDefaultHandlers();
    }

    @Override
    public CompletableFuture<Void> sendMessage(BridgeMessage message) {
        return network.sendMessage(message);
    }

    @Override
    public void registerMessageHandler(MessageType type, MessageHandler handler) {
        handlers.put(type, handler);
        LOGGER.info("Registered handler for message type: " + type);
    }

    @Override
    public CompletableFuture<EventDefinition> requestEventData(String eventId) {
        return CompletableFuture.completedFuture(
                plugin.getStorage().getEventDefinition(eventId).orElse(null)
        );
    }

    @Override
    public CompletableFuture<EventProgress> requestEventProgress(String eventId, UUID playerId) {
        return CompletableFuture.completedFuture(
                plugin.getStorage().getProgress(playerId, eventId).orElse(null)
        );
    }

    @Override
    public CompletableFuture<UIConfig> requestUIConfig(String eventId) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void notifyButtonClick(String buttonId, String eventId, UUID playerId) {
        LOGGER.info("Button clicked: " + buttonId + " on event " + eventId + " by " + playerId);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    public void handleIncomingMessage(BridgeMessage message, Player player) {
        MessageHandler handler = handlers.get(message.getType());

        if (handler != null) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                LOGGER.severe("Error handling message type: " + message.getType());
                e.printStackTrace();
                sendErrorResponse(message, e.getMessage(), player);
            }
        } else {
            LOGGER.warning("No handler for message type: " + message.getType());
            sendErrorResponse(message, "Unknown message type", player);
        }
    }

    private void registerDefaultHandlers() {
        // NUEVO: Handler para REQUEST_EVENT_DATA que envía TODOS los eventos
        registerMessageHandler(MessageType.REQUEST_EVENT_DATA, message -> {
            UUID playerId = message.getPlayerId();

            // Buscar el jugador
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) {
                LOGGER.warning("Player not found for REQUEST_EVENT_DATA: " + playerId);
                return;
            }

            handleRequestEventData(player, message);


        });



        registerMessageHandler(MessageType.REQUEST_EVENT_PROGRESS, message -> {
            String eventId = message.getPayload().get("event_id");
            UUID playerId = message.getPlayerId();

            plugin.getStorage().getProgress(playerId, eventId).ifPresentOrElse(
                    progress -> {
                        Map<String, String> payload = Map.of(
                                "event_id", eventId,
                                "state", progress.getState().name(),
                                "overall_progress", String.valueOf(progress.getOverallProgress()),
                                "started_at", String.valueOf(progress.getStartedAt())
                        );

                        BridgeMessage response = new PluginBridgeMessage(
                                MessageType.EVENT_PROGRESS_RESPONSE,
                                payload,
                                playerId,
                                message.getMessageId()
                        );

                        sendMessage(response);
                    },
                    () -> LOGGER.warning("Progress not found for event: " + eventId)
            );
        });

        registerMessageHandler(MessageType.UI_BUTTON_CLICKED, message -> {
            String buttonId = message.getPayload().get("button_id");
            String eventId = message.getPayload().get("event_id");

            LOGGER.info("Button clicked: " + buttonId + " on event " + eventId);
        });
        // ✅ NUEVO FASE 4A: Handler para REQUEST_UI_CONFIG
        registerMessageHandler(MessageType.REQUEST_UI_CONFIG, message -> {
            UUID playerId = message.getPlayerId();
            Player player = plugin.getServer().getPlayer(playerId);

            if (player == null) {
                LOGGER.warning("Player not found for REQUEST_UI_CONFIG: " + playerId);
                return;
            }

            handleRequestUIConfig(player, message);
        });



    }


    /**
     * NUEVO MÉTODO: Envía TODOS los eventos al cliente, organizados por estado.
     */
    private void handleRequestEventData(Player player, BridgeMessage message) {
        UUID playerId = player.getUniqueId();

        List<EventDefinition> events = plugin.getStorage().getAllEventDefinitions()
                .values()
                .stream()
                .toList();

        List<Map<String, Object>> eventsList = new ArrayList<>();

        for (EventDefinition eventDef : events) {
            Map<String, Object> eventData = new HashMap<>();

            eventData.put("id", eventDef.getId());
            eventData.put("displayName", eventDef.getDisplayName());
            eventData.put("description", eventDef.getDescription());

            var progressOpt = plugin.getStorage().getProgress(playerId, eventDef.getId());

            if (progressOpt.isPresent()) {
                EventProgress progress = progressOpt.get();

                eventData.put("state", progress.getState().name());
                eventData.put("overallProgress", String.valueOf(progress.getOverallProgress()));
                eventData.put("startedAt", String.valueOf(progress.getStartedAt()));

                if (progress.getState() == EventState.IN_PROGRESS) {
                    for (ObjectiveDefinition objDef : eventDef.getObjectives()) {
                        ObjectiveProgress objProgress = progress.getObjectivesProgress().stream()
                                .filter(op -> op.getObjectiveId().equals(objDef.getId()))
                                .findFirst()
                                .orElse(null);

                        if (objProgress != null && !objProgress.isCompleted()) {
                            eventData.put("currentObjective", objDef.getDescription());
                            eventData.put("currentProgress", objProgress.getCurrentAmount());
                            eventData.put("targetProgress", objProgress.getTargetAmount());
                            break;
                        }
                    }
                }
            } else {
                // Evento AVAILABLE - Enviar info del primer objetivo
                eventData.put("state", EventState.AVAILABLE.name());
                eventData.put("overallProgress", "0.0");
                eventData.put("startedAt", "0");

                // ✅ NUEVO: Agregar descripción del primer objetivo
                if (!eventDef.getObjectives().isEmpty()) {
                    ObjectiveDefinition firstObjective = eventDef.getObjectives().get(0);
                    eventData.put("currentObjective", firstObjective.getDescription());
                    eventData.put("currentProgress", 0);
                    eventData.put("targetProgress", firstObjective.getTargetAmount());
                }
            }

            eventsList.add(eventData);
        }

        try {
            String jsonData = new com.google.gson.Gson().toJson(eventsList);

            Map<String, String> payload = Map.of(
                    "events", jsonData,
                    "count", String.valueOf(eventsList.size())
            );

            BridgeMessage response = new PluginBridgeMessage(
                    MessageType.EVENT_DATA_RESPONSE,
                    payload,
                    playerId,
                    message.getMessageId()
            );

            sendMessage(response);

            LOGGER.info("Sent " + eventsList.size() + " events to player " + player.getName());

        } catch (Exception e) {
            LOGGER.severe("Failed to serialize events: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(message, "Failed to load events", player);
        }
    }


    private void sendErrorResponse(BridgeMessage originalMessage, String errorMessage, Player player) {
        Map<String, String> payload = Map.of(
                "error_code", "PROCESSING_ERROR",
                "message", errorMessage
        );

        BridgeMessage response = new PluginBridgeMessage(
                MessageType.ERROR,
                payload,
                player.getUniqueId(),
                originalMessage.getMessageId()
        );

        sendMessage(response);
    }

    /**
     * Notifica progreso actualizado a un jugador (con nombres legibles).
     */
    public void notifyProgressUpdate(UUID playerId, String eventId, String objectiveId, int current, int target, String description) {
        Map<String, String> payload = Map.of(
                "event_id", eventId,
                "objective_id", objectiveId,
                "current", String.valueOf(current),
                "target", String.valueOf(target),
                "description", description
        );

        BridgeMessage message = new PluginBridgeMessageMinimal(
                MessageType.PROGRESS_UPDATE,
                payload,
                playerId
        );

        sendMessage(message);
    }

    public PluginNetworkHandler getNetworkHandler() {
        return network;
    }

    /**
     * Versión mínima de BridgeMessage para notificaciones.
     */
    private static class PluginBridgeMessageMinimal implements BridgeMessage {
        private final MessageType type;
        private final Map<String, String> payload;
        private final UUID playerId;

        public PluginBridgeMessageMinimal(MessageType type, Map<String, String> payload, UUID playerId) {
            this.type = type;
            this.payload = Collections.unmodifiableMap(payload);
            this.playerId = playerId;
        }

        @Override
        public MessageType getType() {
            return type;
        }

        @Override
        public Map<String, String> getPayload() {
            return payload;
        }

        @Override
        public UUID getPlayerId() {
            return playerId;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public UUID getMessageId() {
            return null;
        }

        @Override
        public UUID getReplyToMessageId() {
            return null;
        }
    }

    /**
     * FASE 4A: Envía la configuración de UI al cliente.
     */
    private void handleRequestUIConfig(Player player, BridgeMessage message) {
        String uiId = message.getPayload().get("ui_id");

        if (uiId == null || uiId.isEmpty()) {
            // Si no especifica, enviar la UI por defecto
            uiId = "default_event_list";
        }

        UIConfig uiConfig = plugin.getUIConfigs().get(uiId);

        if (uiConfig == null) {
            LOGGER.warning("UI config not found: " + uiId);
            sendErrorResponse(message, "UI not found: " + uiId, player);
            return;
        }

        try {
            // Serializar UIConfig a JSON para enviar
            com.google.gson.Gson gson = new com.google.gson.Gson();

            Map<String, Object> uiData = new HashMap<>();
            uiData.put("id", uiConfig.getId());
            uiData.put("title", uiConfig.getTitle());
            uiData.put("screen_width", uiConfig.getScreenWidth());
            uiData.put("screen_height", uiConfig.getScreenHeight());
            uiData.put("associated_event_id", uiConfig.getAssociatedEventId());
            uiData.put("screen_properties", uiConfig.getScreenProperties());

            // Serializar elementos
            List<Map<String, Object>> elementsData = new ArrayList<>();
            for (com.eventui.api.ui.UIElement element : uiConfig.getRootElements()) {
                elementsData.add(serializeUIElement(element));
            }
            uiData.put("elements", elementsData);

            String jsonData = gson.toJson(uiData);

            Map<String, String> payload = Map.of(
                    "ui_id", uiConfig.getId(),
                    "ui_data", jsonData
            );

            BridgeMessage response = new PluginBridgeMessage(
                    MessageType.UI_CONFIG_RESPONSE,
                    payload,
                    player.getUniqueId(),
                    message.getMessageId()
            );

            sendMessage(response);

            LOGGER.info("Sent UI config: " + uiId + " to player " + player.getName());

        } catch (Exception e) {
            LOGGER.severe("Failed to serialize UI config: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(message, "Failed to load UI", player);
        }
    }

    /**
     * Serializa un UIElement a Map.
     */
    private Map<String, Object> serializeUIElement(com.eventui.api.ui.UIElement element) {
        Map<String, Object> data = new HashMap<>();

        data.put("id", element.getId());
        data.put("type", element.getType().name());
        data.put("x", element.getX());
        data.put("y", element.getY());
        data.put("width", element.getWidth());
        data.put("height", element.getHeight());
        data.put("properties", element.getProperties());
        data.put("visible", element.isVisible());
        data.put("z_index", element.getZIndex());

        // Serializar children recursivamente
        List<Map<String, Object>> childrenData = new ArrayList<>();
        for (com.eventui.api.ui.UIElement child : element.getChildren()) {
            childrenData.add(serializeUIElement(child));
        }
        data.put("children", childrenData);

        return data;
    }

}
