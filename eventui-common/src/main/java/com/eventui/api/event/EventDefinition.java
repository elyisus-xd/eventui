package com.eventui.api.event;

import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveGroupDefinition;

import java.util.List;
import java.util.Map;

/**
 * Contrato que define QUÉ ES un evento.*
 * ARQUITECTURA:
 * - Define estructura, NO comportamiento
 * - NO contiene progreso ni estado (eso va en EventProgress)
 * - Es inmutable una vez cargado del JSON
 * - El PLUGIN carga estos datos, el MOD los consume
 */
public interface EventDefinition {

    /**
     * @return ID único del evento (ej: "event_tutorial_mining")
     */
    String getId();

    /**
     * @return Nombre visible del evento (puede tener códigos de color)
     */
    String getDisplayName();

    /**
     * @return Descripción del evento (texto corto)
     */
    String getDescription();

    /**
     * @return Lista INMUTABLE de objetivos que componen este evento
     */
    List<ObjectiveDefinition> getObjectives();

    /**
     * Recursos UI asociados a este evento (paths de imágenes, etc.)*
     * Ejemplos de keys:
     * - "icon": "textures/events/mining_icon.png"
     * - "background": "textures/events/mining_bg.png"
     * - "banner": "textures/events/mining_banner.png"
     *
     * @return Mapa inmutable de recursos
     */
    Map<String, String> getUIResources();

    /**
     * Metadata adicional configurable desde JSON*
     * Ejemplos:
     * - "category": "tutorial"
     * - "required_level": "5"
     * - "repeatable": "true"
     *
     * @return Mapa inmutable de propiedades custom
     */
    Map<String, String> getMetadata();

    List<String> getDependencies(); // Lista de IDs de eventos prerequisito


    /**
     * @return Lista de grupos de objetivos (puede estar vacía si solo hay objetivos simples)
     */
    default List<ObjectiveGroupDefinition> getObjectiveGroups() {
        return List.of();
    }

    /**
     * @return Si el evento tiene grupos compuestos configurados
     */
    default boolean hasObjectiveGroups() {
        return !getObjectiveGroups().isEmpty();
    }
}
