package com.eventui.core.event;

import com.eventui.api.event.EventDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Verifica dependencies entre eventos.
 */
public class DependencyChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyChecker.class);

    /**
     * Verifica si un evento está bloqueado por dependencies no cumplidas.
     *
     * @param eventDef Definición del evento
     * @param completedEventIds IDs de eventos ya completados
     * @return true si está bloqueado
     */
    public static boolean isLocked(EventDefinition eventDef, Set<String> completedEventIds) {
        List<String> dependencies = eventDef.getDependencies();

        if (dependencies.isEmpty()) {
            return false; // Sin dependencies = no bloqueado
        }

        // Verificar si TODAS las dependencies están completadas
        for (String depId : dependencies) {
            if (!completedEventIds.contains(depId)) {
                LOGGER.debug("Event {} is locked. Missing dependency: {}", eventDef.getId(), depId);
                return true; // Falta al menos una dependency
            }
        }

        LOGGER.debug("Event {} is unlocked. All dependencies met.", eventDef.getId());
        return false;
    }

    /**
     * Obtiene las dependencies faltantes de un evento.
     *
     * @param eventDef Definición del evento
     * @param completedEventIds IDs de eventos ya completados
     * @return Lista de IDs de dependencies faltantes
     */
    public static List<String> getMissingDependencies(EventDefinition eventDef, Set<String> completedEventIds) {
        List<String> missing = new ArrayList<>();

        for (String depId : eventDef.getDependencies()) {
            if (!completedEventIds.contains(depId)) {
                missing.add(depId);
            }
        }

        return missing;
    }

    /**
     * Verifica si un evento recién completado desbloquea otros eventos.
     *
     * @param completedEventId ID del evento recién completado
     * @param allEvents Todas las definiciones de eventos
     * @param completedEventIds IDs de todos los eventos completados (incluyendo el nuevo)
     * @return Lista de IDs de eventos recién desbloqueados
     */
    public static List<String> getUnlockedEvents(String completedEventId,
                                                 List<EventDefinition> allEvents,
                                                 Set<String> completedEventIds) {
        List<String> unlocked = new ArrayList<>();

        for (EventDefinition event : allEvents) {
            // Debe tener dependencies
            if (event.getDependencies().isEmpty()) continue;

            // Debe depender del evento recién completado
            if (!event.getDependencies().contains(completedEventId)) continue;

            // Ahora debe estar desbloqueado
            if (!isLocked(event, completedEventIds)) {
                unlocked.add(event.getId());
                LOGGER.info("Event {} has been unlocked by completing {}", event.getId(), completedEventId);
            }
        }

        return unlocked;
    }
}
