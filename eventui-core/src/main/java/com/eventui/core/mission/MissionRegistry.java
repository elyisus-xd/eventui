package com.eventui.core.mission;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro central de todas las definiciones de misiones.
 * Actúa como "source of truth" de qué misiones existen.
 */
public class MissionRegistry {
    private final Map<String, Mission> missions;

    public MissionRegistry() {
        this.missions = new ConcurrentHashMap<>();
    }

    /**
     * Registra una nueva misión.
     *
     * @param mission Misión a registrar
     * @throws IllegalArgumentException si ya existe una misión con ese ID
     */
    public void register(Mission mission) {
        if (missions.containsKey(mission.getId())) {
            throw new IllegalArgumentException("Mission already registered: " + mission.getId());
        }
        missions.put(mission.getId(), mission);
    }

    /**
     * Obtiene una misión por ID.
     *
     * @param id ID de la misión
     * @return Optional con la misión, o empty si no existe
     */
    public Optional<Mission> get(String id) {
        return Optional.ofNullable(missions.get(id));
    }

    /**
     * @return Todas las misiones registradas (copia inmutable)
     */
    public Collection<Mission> getAll() {
        return List.copyOf(missions.values());
    }

    /**
     * Obtiene misiones por categoría.
     *
     * @param category Categoría a filtrar
     * @return Lista de misiones en esa categoría
     */
    public List<Mission> getByCategory(String category) {
        return missions.values().stream()
                .filter(m -> category.equals(m.getCategory()))
                .toList();
    }

    /**
     * Verifica si una misión existe.
     *
     * @param id ID de la misión
     * @return true si existe
     */
    public boolean exists(String id) {
        return missions.containsKey(id);
    }

    /**
     * @return Cantidad de misiones registradas
     */
    public int size() {
        return missions.size();
    }

    /**
     * Limpia todas las misiones (útil para testing o recargas).
     */
    public void clear() {
        missions.clear();
    }

    /**
     * Reemplaza una misión existente (útil para hot-reload).
     *
     * @param mission Nueva definición de la misión
     */
    public void update(Mission mission) {
        missions.put(mission.getId(), mission);
    }
}
