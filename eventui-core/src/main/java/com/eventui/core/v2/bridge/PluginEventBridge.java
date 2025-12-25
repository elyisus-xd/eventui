package com.eventui.core.v2.bridge;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.bridge.EventBridge;
import com.eventui.api.bridge.MessageType;
import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.api.ui.UIConfig;
import com.eventui.core.v2.EventUIPlugin;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
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
        registerMessageHandler(MessageType.REQUEST_EVENT_DATA, message -> {
            String eventId = message.getPayload().get("event_id");

            plugin.getStorage().getEventDefinition(eventId).ifPresentOrElse(
                    event -> {
                        Map<String, String> payload = Map.of(
                                "event_id", event.getId(),
                                "display_name", event.getDisplayName(),
                                "description", event.getDescription(),
                                "objectives_count", String.valueOf(event.getObjectives().size())
                        );

                        BridgeMessage response = new PluginBridgeMessage(
                                MessageType.EVENT_DATA_RESPONSE,
                                payload,
                                message.getPlayerId(),
                                message.getMessageId()
                        );

                        sendMessage(response);
                    },
                    () -> LOGGER.warning("Event not found: " + eventId)
            );
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
        String objectiveDescription;
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
