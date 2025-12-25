package com.eventui.fabric.client.bridge;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caché local de eventos y progreso en el cliente.
 *
 * ARQUITECTURA:
 * - Reduce requests al servidor
 * - Mantiene datos sincronizados
 * - Gestiona pending requests
 */
public class ClientEventCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEventCache.class);

    // Caché de definiciones de eventos
    private final Map<String, EventDefinition> eventDefinitions;

    // Caché de progreso de eventos
    private final Map<String, EventProgress> eventProgress;

    // Pending requests esperando respuesta
    private final Map<UUID, CompletableFuture<?>> pendingRequests;

    public ClientEventCache() {
        this.eventDefinitions = new ConcurrentHashMap<>();
        this.eventProgress = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Registra un request pendiente.
     */
    public void registerPendingRequest(UUID messageId, CompletableFuture<?> future) {
        pendingRequests.put(messageId, future);
    }

    /**
     * Completa un request pendiente con datos.
     */
    @SuppressWarnings("unchecked")
    private <T> void completePendingRequest(UUID messageId, T data) {
        CompletableFuture<?> future = pendingRequests.remove(messageId);

        if (future != null) {
            ((CompletableFuture<T>) future).complete(data);
        }
    }

    /**
     * Falla un request pendiente.
     */
    private void failPendingRequest(UUID messageId, Throwable error) {
        CompletableFuture<?> future = pendingRequests.remove(messageId);

        if (future != null) {
            future.completeExceptionally(error);
        }
    }

    /**
     * Maneja respuesta de EVENT_DATA.
     */
    public void handleEventDataResponse(BridgeMessage message) {
        UUID replyTo = message.getReplyToMessageId();

        if (replyTo == null) {
            LOGGER.warn("EVENT_DATA_RESPONSE without replyTo ID");
            return;
        }

        try {
            // TODO: Deserializar EventDefinition del payload
            // Por ahora usamos un placeholder
            String eventId = message.getPayload().get("event_id");

            LOGGER.info("Received event data for: {}", eventId);

            // Completar el future pendiente
            // completePendingRequest(replyTo, eventDefinition);

        } catch (Exception e) {
            LOGGER.error("Failed to parse EVENT_DATA_RESPONSE", e);
            failPendingRequest(replyTo, e);
        }
    }

    /**
     * Maneja respuesta de PROGRESS.
     */
    public void handleProgressResponse(BridgeMessage message) {
        UUID replyTo = message.getReplyToMessageId();

        if (replyTo == null) {
            LOGGER.warn("EVENT_PROGRESS_RESPONSE without replyTo ID");
            return;
        }

        try {
            String eventId = message.getPayload().get("event_id");

            LOGGER.info("Received progress data for: {}", eventId);

            // TODO: Deserializar EventProgress del payload
            // completePendingRequest(replyTo, eventProgress);

        } catch (Exception e) {
            LOGGER.error("Failed to parse EVENT_PROGRESS_RESPONSE", e);
            failPendingRequest(replyTo, e);
        }
    }

    /**
     * Invalida el caché de un evento específico.
     */
    public void invalidateEvent(String eventId) {
        eventDefinitions.remove(eventId);
        LOGGER.debug("Invalidated event cache: {}", eventId);
    }

    /**
     * Invalida el caché de progreso.
     */
    public void invalidateProgress(String eventId) {
        eventProgress.remove(eventId);
        LOGGER.debug("Invalidated progress cache: {}", eventId);
    }

    /**
     * Limpia todo el caché.
     */
    public void clear() {
        eventDefinitions.clear();
        eventProgress.clear();

        // Fallar todos los pending requests
        pendingRequests.values().forEach(future ->
                future.completeExceptionally(new IllegalStateException("Bridge disconnected"))
        );
        pendingRequests.clear();

        LOGGER.info("Cache cleared");
    }

    /**
     * Obtiene un evento del caché.
     */
    public EventDefinition getCachedEvent(String eventId) {
        return eventDefinitions.get(eventId);
    }

    /**
     * Obtiene progreso del caché.
     */
    public EventProgress getCachedProgress(String eventId) {
        return eventProgress.get(eventId);
    }
}
