package com.eventui.common.contract.persistence;

import java.time.Duration;
import java.util.Set;

/**
 * Política de guardado que el Plugin de Persistencia implementa.
 * Define CUÁNDO y CÓMO se guardan los datos.
 * El Core NO decide cuándo guardar, solo emite eventos.
 */
public interface SavePolicy {

    /**
     * Determina si un evento debe guardarse inmediatamente.
     *
     * @param event Evento a evaluar
     * @return true si debe guardarse al instante
     */
    boolean shouldSaveImmediately(PersistenceEvent event);

    /**
     * @return Intervalo para guardado por lotes (batch)
     */
    Duration getBatchInterval();

    /**
     * @return Tipos de eventos que se consideran críticos
     */
    Set<Class<? extends PersistenceEvent>> getCriticalEventTypes();

    /**
     * @return Máximo de eventos en cola antes de forzar guardado
     */
    int getMaxQueueSize();

    /**
     * @return true si se debe guardar al desconectar el jugador
     */
    default boolean shouldSaveOnDisconnect() {
        return true;
    }

    /**
     * @return true si se debe guardar periódicamente aunque no haya cambios
     */
    default boolean shouldSavePeriodically() {
        return true;
    }
}