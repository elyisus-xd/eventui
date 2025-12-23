package com.eventui.api.bridge;

/**
 * Tipos de mensajes que pueden intercambiarse entre MOD y PLUGIN.
 * ARQUITECTURA:
 * - Define el protocolo de comunicación
 * - MOD → PLUGIN: solicitudes de datos, notificaciones de acciones UI
 * - PLUGIN → MOD: actualizaciones de progreso, datos de eventos
 */
public enum MessageType {
    // ========== MOD → PLUGIN ==========

    /**
     * Solicitar datos de un evento específico
     * Payload: {"event_id": "..."}
     */
    REQUEST_EVENT_DATA,

    /**
     * Solicitar progreso de un evento para el jugador
     * Payload: {"event_id": "...", "player_uuid": "..."}
     */
    REQUEST_EVENT_PROGRESS,

    /**
     * Solicitar configuración UI de un evento
     * Payload: {"event_id": "..."}
     */
    REQUEST_UI_CONFIG,

    /**
     * Notificar que el jugador hizo click en un botón
     * Payload: {"button_id": "...", "event_id": "..."}
     */
    UI_BUTTON_CLICKED,

    /**
     * Notificar que el jugador abrió la pantalla de evento
     * Payload: {"event_id": "...", "player_uuid": "..."}
     */
    UI_SCREEN_OPENED,

    /**
     * Notificar que el jugador cerró la pantalla
     * Payload: {"event_id": "..."}
     */
    UI_SCREEN_CLOSED,

    // ========== PLUGIN → MOD ==========

    /**
     * Respuesta con datos de un evento
     * Payload: serialización de EventDefinition
     */
    EVENT_DATA_RESPONSE,

    /**
     * Respuesta con progreso de evento
     * Payload: serialización de EventProgress
     */
    EVENT_PROGRESS_RESPONSE,

    /**
     * Respuesta con configuración UI
     * Payload: serialización de UIConfig
     */
    UI_CONFIG_RESPONSE,

    /**
     * Actualización de progreso (push del PLUGIN)
     * Payload: {"event_id": "...", "player_uuid": "...", "objective_id": "...", "current": "5", "target": "10"}
     */
    PROGRESS_UPDATE,

    /**
     * Notificación de cambio de estado de evento
     * Payload: {"event_id": "...", "player_uuid": "...", "new_state": "COMPLETED"}
     */
    EVENT_STATE_CHANGED,

    /**
     * Error en el procesamiento de un mensaje
     * Payload: {"error_code": "...", "message": "..."}
     */
    ERROR
}
