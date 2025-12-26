package com.eventui.api.objective;

import java.util.Map;

/**
 * Contrato que define QUÉ ES un objetivo dentro de un evento.*
 * ARQUITECTURA:
 * - Define estructura, NO implementa tracking
 * - El PLUGIN lee esta definición del JSON
 * - El PLUGIN implementa la lógica según el ObjectiveType
 * - El MOD solo usa esta info para mostrar en UI
 */
public interface ObjectiveDefinition {

    /**
     * @return ID único del objetivo dentro del evento
     */
    String getId();

    /**
     * @return Tipo de objetivo (determina qué sistema lo trackea)
     */
    ObjectiveType getType();

    /**
     * @return Descripción visible del objetivo
     * Ejemplo: "Mina 10 bloques de diamante"
     */
    String getDescription();

    /**
     * @return Cantidad objetivo requerida para completar
     * Ejemplo: 10 (para "mina 10 diamantes")
     */
    int getTargetAmount();

    /**
     * Configuración específica del tipo de objetivo*
     * Ejemplos según tipo:
     * - MINE_BLOCK: {"block_id": "minecraft:diamond_ore"}
     * - KILL_ENTITY: {"entity_type": "minecraft:zombie", "min_distance": "50"}
     * - REACH_LOCATION: {"x": "100", "y": "64", "z": "200", "radius": "5"}
     *
     * @return Mapa inmutable con parámetros específicos del objetivo
     */
    Map<String, String> getParameters();

    /**
     * Recursos UI para este objetivo (opcional)*
     * Ejemplo: {"icon": "textures/objectives/diamond.png"}
     *
     * @return Mapa inmutable de recursos
     */
    Map<String, String> getUIResources();

    /**
     * @return Si este objetivo es opcional (default: false)
     */
    default boolean isOptional() {
        return false;
    }
}
