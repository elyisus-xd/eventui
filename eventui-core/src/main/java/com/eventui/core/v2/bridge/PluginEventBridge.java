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
}
