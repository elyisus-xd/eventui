package com.eventui.core.loader;

import com.eventui.core.mission.*;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * Carga definiciones de misiones desde archivos YAML.
 */
public class MissionDefinitionLoader {
    private final Yaml yaml;
    private final MissionValidator validator;

    public MissionDefinitionLoader() {
        this.yaml = new Yaml();
        this.validator = new MissionValidator();
    }

    /**
     * Carga una misión desde un InputStream YAML.
     *
     * @param input Stream del archivo YAML
     * @return Mission parseada y validada
     * @throws MissionLoadException si el formato es inválido
     */
    public Mission load(InputStream input) throws MissionLoadException {
        try {
            Map<String, Object> data = yaml.load(input);
            return parseMission(data);
        } catch (Exception e) {
            throw new MissionLoadException("Failed to load mission: " + e.getMessage(), e);
        }
    }

    /**
     * Carga múltiples misiones desde un directorio.
     *
     * @param inputs Lista de streams de archivos YAML
     * @return Lista de misiones cargadas
     */
    public List<Mission> loadAll(List<InputStream> inputs) {
        List<Mission> missions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (InputStream input : inputs) {
            try {
                Mission mission = load(input);
                missions.add(mission);
            } catch (MissionLoadException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            System.err.println("Errors loading missions:");
            errors.forEach(System.err::println);
        }

        return missions;
    }

    /**
     * Parsea un mapa YAML a objeto Mission.
     */
    private Mission parseMission(Map<String, Object> data) throws MissionLoadException {
        // Campos obligatorios
        String id = getRequired(data, "id");
        String title = getRequired(data, "title");
        String description = getRequired(data, "description");

        // Campos opcionales
        List<String> prerequisites = getList(data, "prerequisites", new ArrayList<>());
        boolean repeatable = getBoolean(data, "repeatable", false);
        String category = getString(data, "category", "general");
        String difficulty = getString(data, "difficulty", "normal");

        // Parsear objetivos
        List<MissionObjective> objectives = parseObjectives(data);

        // Parsear recompensas
        List<MissionReward> rewards = parseRewards(data);

        // Metadata adicional
        Map<String, Object> metadata = getMap(data, "metadata", new HashMap<>());

        // Crear misión
        Mission mission = new Mission(
                id, title, description, objectives, prerequisites,
                rewards, repeatable, category, difficulty, metadata
        );

        // Validar
        ValidationResult result = validator.validate(mission);
        if (!result.isValid()) {
            throw new MissionLoadException("Validation failed: " + String.join(", ", result.getErrors()));
        }

        return mission;
    }

    /**
     * Parsea la lista de objetivos.
     */
    @SuppressWarnings("unchecked")
    private List<MissionObjective> parseObjectives(Map<String, Object> data) throws MissionLoadException {
        List<Map<String, Object>> objectivesData = (List<Map<String, Object>>) data.get("objectives");
        if (objectivesData == null || objectivesData.isEmpty()) {
            throw new MissionLoadException("Mission must have at least one objective");
        }

        List<MissionObjective> objectives = new ArrayList<>();
        for (Map<String, Object> objData : objectivesData) {
            String id = getRequired(objData, "id");
            String type = getRequired(objData, "type");
            String target = getRequired(objData, "target");
            int count = getInt(objData, "count", 1);
            String dimension = getString(objData, "dimension", null);

            MissionObjective.ObjectiveType objectiveType;
            try {
                objectiveType = MissionObjective.ObjectiveType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new MissionLoadException("Invalid objective type: " + type);
            }

            objectives.add(new MissionObjective(id, objectiveType, target, count, dimension));
        }

        return objectives;
    }

    /**
     * Parsea la lista de recompensas.
     */
    @SuppressWarnings("unchecked")
    private List<MissionReward> parseRewards(Map<String, Object> data) {
        Map<String, Object> rewardsData = (Map<String, Object>) data.get("rewards");
        if (rewardsData == null) {
            return new ArrayList<>();
        }

        List<MissionReward> rewards = new ArrayList<>();

        // Items
        List<Map<String, Object>> items = (List<Map<String, Object>>) rewardsData.get("items");
        if (items != null) {
            for (Map<String, Object> itemData : items) {
                String itemId = (String) itemData.get("item");
                int count = getInt(itemData, "count", 1);
                rewards.add(MissionReward.item(itemId, count));
            }
        }

        // Experience
        Object exp = rewardsData.get("experience");
        if (exp instanceof Integer expValue) {
            rewards.add(MissionReward.experience(expValue));
        }

        return rewards;
    }

    // Métodos auxiliares de parsing

    private String getRequired(Map<String, Object> map, String key) throws MissionLoadException {
        Object value = map.get(key);
        if (value == null) {
            throw new MissionLoadException("Missing required field: " + key);
        }
        return value.toString();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key, List<String> defaultValue) {
        Object value = map.get(key);
        return value instanceof List ? (List<String>) value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key, Map<String, Object> defaultValue) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : defaultValue;
    }

    /**
     * Excepción lanzada cuando falla la carga de una misión.
     */
    public static class MissionLoadException extends Exception {
        public MissionLoadException(String message) {
            super(message);
        }

        public MissionLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

