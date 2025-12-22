package com.eventui.common.contract.signal;

import java.util.function.Consumer;

/**
 * Bus de señales del juego.
 * El Motor de Señales emite señales y el Core se suscribe para procesarlas.
 * Patrón Publisher-Subscriber para eventos del juego.
 */
public interface SignalBus {

    /**
     * Suscribe un handler para un tipo específico de señal.
     *
     * @param signalType Clase de la señal (ej: EntityKilled.class)
     * @param handler Función que procesa la señal
     * @param <T> Tipo de señal
     */
    <T extends GameSignal> void subscribe(Class<T> signalType, Consumer<T> handler);

    /**
     * Suscribe un handler para TODAS las señales.
     *
     * @param handler Función que procesa cualquier señal
     */
    void subscribeAll(Consumer<GameSignal> handler);

    /**
     * Cancela la suscripción de un handler.
     *
     * @param signalType Tipo de señal
     * @param handler Handler a desuscribir
     * @param <T> Tipo de señal
     */
    <T extends GameSignal> void unsubscribe(Class<T> signalType, Consumer<T> handler);

    /**
     * Emite una señal a todos los suscriptores.
     * Solo el Motor de Señales debe llamar a este método.
     *
     * @param signal Señal a emitir
     */
    void emit(GameSignal signal);

    /**
     * Limpia todos los suscriptores.
     */
    void clear();
}