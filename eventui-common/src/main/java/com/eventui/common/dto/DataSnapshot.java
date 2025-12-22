package com.eventui.common.dto;

import java.util.Collections;
import java.util.Map;

/**
 * Snapshot inmutable de datos enviados del ViewModel a la UI.*
 * La UI recibe SOLO primitivos, strings, listas o mapas.
 * Nunca recibe objetos del dominio del Core.
 */
public record DataSnapshot(
        String key,
        Map<String, Object> data,
        long timestamp
) {

    /**
     * Constructor que asegura inmutabilidad.
     */
    public DataSnapshot {
        data = Collections.unmodifiableMap(data);
    }

    /**
     * @return Valor específico del snapshot
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * @return true si contiene la clave
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }
}
