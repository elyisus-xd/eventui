package com.eventui.fabric.client.bridge;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.bridge.EventBridge;
import com.eventui.api.bridge.MessageType;
import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.api.ui.UIConfig;
import com.eventui.fabric.client.viewmodel.EventViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación del EventBridge para el lado del CLIENTE (Fabric MOD).*
 * ARQUITECTURA:
 * - Envía solicitudes al PLUGIN via red
 * - Recibe actualizaciones del PLUGIN
 * - Mantiene caché local de eventos
 */
public class ClientEventBridge implements EventBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventBridge.class);
    private static ClientEventBridge instance;

    private final Map<MessageType, MessageHandler> handlers;
    private final ClientEventCache cache;
    private final NetworkHandler network;
    private boolean connected;
    private EventViewModel globalViewModel;

    public ClientEventBridge() {
        this.handlers = new ConcurrentHashMap<>();
        this.cache = new ClientEventCache();
        this.network = new NetworkHandler(this);
        this.connected = true;

        registerDefaultHandlers();

        LOGGER.info("ClientEventBridge initialized and connected");
    }

    public static ClientEventBridge getInstance() {
        if (instance == null) {
            instance = new ClientEventBridge();
        }
        return instance;
    }

    /**
     * Obtiene o crea el ViewModel global.
     */
    public EventViewModel getOrCreateViewModel(UUID playerId) {
        if (globalViewModel == null) {
            globalViewModel = new EventViewModel(playerId);
        }
        return globalViewModel;
    }


    @Override
    public CompletableFuture<Void> sendMessage(BridgeMessage message) {
        // ✅ SIN VALIDACIÓN DE CONEXIÓN
        LOGGER.debug("Sending message type: {}", message.getType());
        return network.sendMessage(message);
    }


    @Override
    public void registerMessageHandler(MessageType type, MessageHandler handler) {
        handlers.put(type, handler);
        LOGGER.debug("Registered handler for message type: {}", type);
    }

    @Override
    public CompletableFuture<EventDefinition> requestEventData(String eventId) {
        UUID playerId = getLocalPlayerId();

        BridgeMessage request = new BridgeMessageImpl(
                MessageType.REQUEST_EVENT_DATA,
                Map.of("event_id", eventId),
                playerId
        );

        CompletableFuture<EventDefinition> future = new CompletableFuture<>();

        // Registrar handler temporal para la respuesta
        UUID messageId = request.getMessageId();
        cache.registerPendingRequest(messageId, future);

        sendMessage(request).exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
        });

        return future;
    }

    @Override
    public CompletableFuture<EventProgress> requestEventProgress(String eventId, UUID playerId) {
        BridgeMessage request = new BridgeMessageImpl(
                MessageType.REQUEST_EVENT_PROGRESS,
                Map.of(
                        "event_id", eventId,
                        "player_uuid", playerId.toString()
                ),
                playerId
        );

        CompletableFuture<EventProgress> future = new CompletableFuture<>();
        cache.registerPendingRequest(request.getMessageId(), future);

        sendMessage(request).exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
        });

        return future;
    }

    @Override
    public CompletableFuture<UIConfig> requestUIConfig(String eventId) {
        UUID playerId = getLocalPlayerId();

        BridgeMessage request = new BridgeMessageImpl(
                MessageType.REQUEST_UI_CONFIG,
                Map.of("event_id", eventId),
                playerId
        );

        CompletableFuture<UIConfig> future = new CompletableFuture<>();
        cache.registerPendingRequest(request.getMessageId(), future);

        sendMessage(request).exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
        });

        return future;
    }

    @Override
    public void notifyButtonClick(String buttonId, String eventId, UUID playerId) {
        BridgeMessage message = new BridgeMessageImpl(
                MessageType.UI_BUTTON_CLICKED,
                Map.of(
                        "button_id", buttonId,
                        "event_id", eventId
                ),
                playerId
        );

        sendMessage(message);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Llamado cuando se recibe un mensaje del servidor.
     */
    public void handleIncomingMessage(BridgeMessage message) {
        MessageHandler handler = handlers.get(message.getType());

        if (handler != null) {
            try {
                handler.handle(message);
            } catch (Exception e) {
                LOGGER.error("Error handling message type: {}", message.getType(), e);
            }
        } else {
            LOGGER.warn("No handler registered for message type: {}", message.getType());
        }
    }

    /**
     * Registra handlers por defecto para respuestas del servidor.
     */
    private void registerDefaultHandlers() {
        // ✅ NUEVO: Handler para UI_CONFIG_RESPONSE
        registerMessageHandler(MessageType.UI_CONFIG_RESPONSE, message -> {
            String uiId = message.getPayload().get("ui_id");
            String uiDataJson = message.getPayload().get("ui_data");

            LOGGER.info("Received UI_CONFIG_RESPONSE for: {}", uiId);

            // Guardar en caché para uso posterior
            cache.cacheUIConfig(uiId, uiDataJson);
        });

        // Handler para respuestas de datos de eventos
        registerMessageHandler(MessageType.EVENT_DATA_RESPONSE, message -> {
            cache.handleEventDataResponse(message);
        });

        // Handler para respuestas de progreso
        registerMessageHandler(MessageType.EVENT_PROGRESS_RESPONSE, message -> {
            cache.handleProgressResponse(message);
        });

        // Handler para actualizaciones de progreso en tiempo real
        registerMessageHandler(MessageType.PROGRESS_UPDATE, message -> {
            LOGGER.info("Progress update received: {}", message.getPayload());
            cache.invalidateProgress(message.getPayload().get("event_id"));
        });

        // Handler para cambios de estado
        registerMessageHandler(MessageType.EVENT_STATE_CHANGED, message -> {
            LOGGER.info("Event state changed: {}", message.getPayload());
            cache.invalidateEvent(message.getPayload().get("event_id"));
        });
    }


    /**
     * Obtiene el UUID del jugador local.
     */
    private UUID getLocalPlayerId() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Local player not available");
        }
        return player.getUUID();
    }

    /**
     * Marca el bridge como conectado.
     */
    public void onConnect() {
        this.connected = true;
        LOGGER.info("ClientEventBridge connected");
    }

    /**
     * Marca el bridge como desconectado.
     */
    public void onDisconnect() {
        this.connected = false;
        cache.clear();
        LOGGER.info("ClientEventBridge disconnected");
    }

    public ClientEventCache getCache() {
        return cache;
    }
}
