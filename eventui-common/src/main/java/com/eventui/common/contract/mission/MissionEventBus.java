package com.eventui.common.contract.mission;

import java.util.function.Consumer;

/**
 * Bus de eventos para misiones.
 * El Core publica eventos y el ViewModel se suscribe.*
 * Patrón Publisher-Subscriber desacoplado.
 */
public interface MissionEventBus {

    /**
     * Suscribe un listener para todos los eventos de misiones.
     *
     * @param listener Función que recibe eventos
     */
    void subscribe(Consumer<MissionEvent> listener);

    /**
     * Cancela la suscripción de un listener.
     *
     * @param listener Listener a desuscribir
     */
    void unsubscribe(Consumer<MissionEvent> listener);

    /**
     * Publica un evento a todos los suscriptores.
     * Solo el Core debe llamar a este método.
     *
     * @param event Evento a publicar
     */
    void publish(MissionEvent event);

    /**
     * Limpia todos los suscriptores.
     * Útil para testing o reinicios.
     */
    void clear();
}
