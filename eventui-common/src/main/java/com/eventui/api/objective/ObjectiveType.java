package com.eventui.api.objective;

/**
 * Tipos de objetivos soportados por EventUI.
 *
 * ARQUITECTURA:
 * - Define QUÉ tipos de objetivos existen
 * - Cada tipo tiene su propia lógica de tracking (implementada en PLUGIN)
 * - El MOD solo necesita saber el tipo para mostrar iconos apropiados
 * - Extensible: agregar nuevos tipos no rompe el sistema
 */
public enum ObjectiveType {
    /**
     * Minar bloques específicos
     * Ejemplo: "Mina 10 diamantes"
     */
    MINE_BLOCK,

    /**
     * Matar entidades específicas
     * Ejemplo: "Mata 5 zombies"
     */
    KILL_ENTITY,

    /**
     * Craftear items específicos
     * Ejemplo: "Craftea 1 espada de diamante"
     */
    CRAFT_ITEM,

    /**
     * Recolectar items en el inventario
     * Ejemplo: "Consigue 64 troncos de roble"
     */
    COLLECT_ITEM,

    /**
     * Visitar una ubicación específica
     * Ejemplo: "Ve a las coordenadas X:100, Z:200"
     */
    REACH_LOCATION,

    /**
     * Interactuar con un bloque/entidad
     * Ejemplo: "Habla con el NPC aldeano"
     */
    INTERACT,

    /**
     * Objetivo personalizado (lógica definida por el creador)
     * Ejemplo: "Completa el minijuego parkour"
     */
    CUSTOM
}
