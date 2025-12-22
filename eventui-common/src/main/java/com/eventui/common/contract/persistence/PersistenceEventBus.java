package com.eventui.common.contract.persistence;

import java.util.function.Consumer;

/**
 * Bus de eventos de persistencia.
 * El Core publica eventos y el Plugin se suscribe.
 */
public interface PersistenceEventBus {

    /**
     * Suscribe un listener para eventos de persistencia.
     *
     * @param listener Función que recibe eventos
     */
    void subscribe(Consumer<PersistenceEvent> listener);

    /**
     * Cancela la suscripción.
     *
     * @param listener Listener a desuscribir
     */
    void unsubscribe(Consumer<PersistenceEvent> listener);

    /**
     * Publica un evento de persistencia.
     * Solo el Core debe llamar a este método.
     *
     * @param event Evento a publicar
     */
    void publish(PersistenceEvent event);

    /**
     * Limpia todos los suscriptores.
     */
    void clear();
}