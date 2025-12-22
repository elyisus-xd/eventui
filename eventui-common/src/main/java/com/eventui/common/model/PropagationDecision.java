package com.eventui.common.model;

/**
 * Decisión sobre qué hacer con una señal después de procesarla.
 */
public enum PropagationDecision {
    /**
     * Consumir la señal, no propagar a otros listeners.
     */
    CONSUME,

    /**
     * Permitir que otros listeners también procesen la señal.
     */
    PROPAGATE,

    /**
     * Ignorar la señal (no me interesa, pero otros pueden procesarla).
     */
    IGNORE
}