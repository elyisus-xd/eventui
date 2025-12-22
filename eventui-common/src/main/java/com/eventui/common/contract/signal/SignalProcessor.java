package com.eventui.common.contract.signal;

import com.eventui.common.model.PropagationDecision;

/**
 * Procesador de señales que decide si propagar o consumir.
 * El Core implementa esta interfaz para filtrar señales.
 */
public interface SignalProcessor {

    /**
     * Procesa una señal y decide si debe propagarse a otros listeners.
     *
     * @param signal Señal a procesar
     * @return Decisión de propagación
     */
    PropagationDecision process(GameSignal signal);

    /**
     * @return Prioridad del procesador (mayor = procesa primero)
     */
    default int getPriority() {
        return 0;
    }
}