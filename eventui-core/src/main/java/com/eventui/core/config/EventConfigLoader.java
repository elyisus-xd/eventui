package com.eventui.core.config;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveType;
import com.eventui.core.event.EventDefinitionImpl;
import com.eventui.core.objective.ObjectiveDefinitionImpl;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Carga definiciones de eventos desde archivos YAML.
 * MIGRADO: JSON → YAML para mejor legibilidad.
 */
public class EventConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(EventConfigLoader.class.getName());

    private final Yaml yaml;
    private final File eventsDirectory;

    public EventConfigLoader(File pluginDataFolder) {
        this.yaml = new Yaml();
        this.eventsDirectory = new File(pluginDataFolder, "events");

        if (!eventsDirectory.exists()) {
            eventsDirectory.mkdirs();
            LOGGER.info("Created events directory at: " + eventsDirectory.getAbsolutePath());
        }
    }

    /**
     * Carga todos los eventos del directorio (incluyendo subdirectorios).
     */
    public Map<String, EventDefinition> loadAllEvents() {
        Map<String, EventDefinition> events = new HashMap<>();

        // ✅ NUEVO: Buscar archivos recursivamente
        List<File> files = findYamlFiles(eventsDirectory);

        if (files.isEmpty()) {
            LOGGER.warning("No event files found in: " + eventsDirectory.getAbsolutePath());
            createExampleEventFile();
            return events;
        }

        for (File file : files) {
            try {
                EventDefinition event = loadEventFromFile(file);
                events.put(event.getId(), event);

                // ✅ Mostrar ruta relativa para mejor organización
                String relativePath = eventsDirectory.toPath()
                        .relativize(file.toPath())
                        .toString();
                LOGGER.info("✓ Loaded event: " + event.getId() + " from " + relativePath);

            } catch (Exception e) {
                LOGGER.severe("═══════════════════════════════");
                LOGGER.severe("YAML ERROR in file: " + file.getName());
                LOGGER.severe("Problem: " + e.getMessage());
                LOGGER.severe("Common fixes:");
                LOGGER.severe("  - Check indentation (use spaces, not tabs)");
                LOGGER.severe("  - Verify all strings are properly quoted");
                LOGGER.severe("  - Use YAML validator: https://www.yamllint.com");
                LOGGER.severe("File location: " + file.getAbsolutePath());
                LOGGER.severe("═══════════════════════════════");
            }
        }

        if (events.isEmpty()) {
            LOGGER.warning("No events were loaded successfully. Check the errors above.");
        } else {
            LOGGER.info("Successfully loaded " + events.size() + " event(s) from " +
                    countDirectories(eventsDirectory) + " directory/directories");
        }

        return events;
    }

    /**
     * Cuenta el número de directorios (incluyendo subdirectorios) que contienen archivos YAML.
     *
     * @param directory Directorio raíz
     * @return Número de directorios con archivos
     */
    private int countDirectories(File directory) {
        Set<File> directories = new HashSet<>();
        List<File> files = findYamlFiles(directory);

        for (File file : files) {
            directories.add(file.getParentFile());
        }

        return directories.size();
    }

    /**
     * Busca recursivamente todos los archivos .yml y .yaml en un directorio.
     *
     * @param directory Directorio raíz donde buscar
     * @return Lista de archivos YAML encontrados
     */
    private List<File> findYamlFiles(File directory) {
        List<File> yamlFiles = new ArrayList<>();

        if (!directory.exists() || !directory.isDirectory()) {
            return yamlFiles;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return yamlFiles;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // ✅ Recursión: buscar en subdirectorios
                yamlFiles.addAll(findYamlFiles(file));
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                yamlFiles.add(file);
            }
        }

        return yamlFiles;
    }


    /**
     * Carga un evento desde un archivo específico.
     */
    public EventDefinition loadEventFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Map<String, Object> data = yaml.load(fis);
            return parseEvent(data);
        }
    }

    /**
     * Parsea un mapa YAML a EventDefinition.
     */
    @SuppressWarnings("unchecked")
    private EventDefinition parseEvent(Map<String, Object> data) {
        String id = (String) data.get("id");

        // ✅ SOPORTAR AMBOS FORMATOS
        String displayName = (String) data.get("displayName");
        if (displayName == null) {
            displayName = (String) data.get("display_name");
        }

        String description = (String) data.get("description");

        // Parsear objetivos
        List<ObjectiveDefinition> objectives = new ArrayList<>();
        List<Map<String, Object>> objectivesList = (List<Map<String, Object>>) data.get("objectives");

        if (objectivesList != null) {
            for (Map<String, Object> objData : objectivesList) {
                objectives.add(parseObjective(objData));
            }
        }

        // Parsear UI resources
        Map<String, String> uiResources = new HashMap<>();
        Map<String, Object> uiResourcesData = (Map<String, Object>) data.get("ui_resources");
        if (uiResourcesData != null) {
            uiResourcesData.forEach((key, value) -> uiResources.put(key, value.toString()));
        }

        // Parsear metadata
        Map<String, String> metadata = new HashMap<>();
        Map<String, Object> metadataData = (Map<String, Object>) data.get("metadata");
        if (metadataData != null) {
            metadataData.forEach((key, value) -> metadata.put(key, value.toString()));
        }

        // Cargar icon
        String icon = (String) data.getOrDefault("icon", "minecraft:paper");
        metadata.put("icon", icon);

        // ✅ NUEVO: Cargar dependencies
        List<String> dependencies = new ArrayList<>();
        Object depsObj = data.get("dependencies");
        if (depsObj instanceof List) {
            for (Object dep : (List<?>) depsObj) {
                if (dep instanceof String) {
                    dependencies.add((String) dep);
                }
            }
        }

        if (!dependencies.isEmpty()) {
            metadata.put("dependencies", new com.google.gson.Gson().toJson(dependencies));
            LOGGER.info("Event '" + id + "' has " + dependencies.size() + " dependencies: " + dependencies);
        }

        // Cargar rewards
        Map<String, Object> rewards = new HashMap<>();
        Map<String, Object> rewardsData = (Map<String, Object>) data.get("rewards");

        if (rewardsData != null) {
            if (rewardsData.containsKey("xp")) {
                rewards.put("xp", rewardsData.get("xp"));
            }

            if (rewardsData.containsKey("items")) {
                Object itemsObj = rewardsData.get("items");
                if (itemsObj instanceof List) {
                    rewards.put("items", itemsObj);
                }
            }

            if (rewardsData.containsKey("commands")) {
                Object commandsObj = rewardsData.get("commands");
                if (commandsObj instanceof List) {
                    rewards.put("commands", commandsObj);
                }
            }
        }

        if (!rewards.isEmpty()) {
            metadata.put("rewards_data", new com.google.gson.Gson().toJson(rewards));
        }

        return new EventDefinitionImpl(id, displayName, description, objectives, uiResources, metadata, dependencies);
    }

    /**
     * Parsea un objetivo desde YAML.
     */
    @SuppressWarnings("unchecked")
    private ObjectiveDefinition parseObjective(Map<String, Object> data) {
        String id = (String) data.get("id");
        ObjectiveType type = ObjectiveType.valueOf(((String) data.get("type")).toUpperCase());
        String description = (String) data.get("description");

        int targetAmount = 0;
        Map<String, String> parameters = new HashMap<>();

        // ✅ SOPORTAR target_amount (formato viejo)
        if (data.containsKey("target_amount")) {
            targetAmount = ((Number) data.get("target_amount")).intValue();

            // Parameters en formato viejo
            Map<String, Object> parametersData = (Map<String, Object>) data.get("parameters");
            if (parametersData != null) {
                parametersData.forEach((key, value) -> parameters.put(key, value.toString()));
            }
        }
        // ✅ SOPORTAR target {} (formato nuevo)
        else if (data.containsKey("target")) {
            Map<String, Object> targetData = (Map<String, Object>) data.get("target");

            if (targetData != null) {
                // Extraer count
                if (targetData.containsKey("count")) {
                    targetAmount = ((Number) targetData.get("count")).intValue();
                }

                // Convertir todo el target a parameters
                targetData.forEach((key, value) -> {
                    if (!"count".equals(key)) {
                        parameters.put(key, value.toString());
                    }
                });
            }
        }

        // Parsear UI resources
        Map<String, String> uiResources = new HashMap<>();
        Map<String, Object> uiResourcesData = (Map<String, Object>) data.get("ui_resources");
        if (uiResourcesData != null) {
            uiResourcesData.forEach((key, value) -> uiResources.put(key, value.toString()));
        }

        boolean optional = data.containsKey("optional") && (Boolean) data.get("optional");

        return new ObjectiveDefinitionImpl(id, type, description, targetAmount, parameters, uiResources, optional);
    }


    /**
     * Crea un archivo de ejemplo si no existen eventos.
     */
    private void createExampleEventFile() {
        File exampleFile = new File(eventsDirectory, "tutorial_mining.yml");

        if (exampleFile.exists()) {
            return;
        }

        String exampleYaml = """
                id: tutorial-mining
                displayName: "Tutorial de Minería"
                description: "Aprende a minar tus primeros recursos"
                category: "tutorial"
                difficulty: "easy"
                icon: "minecraft:stone_pickaxe"
                repeatable: false
                
                objectives:
                  - id: mine_stone
                    type: MINE_BLOCK
                    description: "Mina 10 bloques de piedra"
                    target:
                      block: "minecraft:stone"
                      count: 10
                
                rewards:
                  xp: 100
                  items:
                    - "minecraft:iron_pickaxe 1"
                    - "minecraft:cooked_beef 5"
                """;

        try {
            java.nio.file.Files.writeString(exampleFile.toPath(), exampleYaml);
            LOGGER.info("Created example event file: " + exampleFile.getName());
        } catch (IOException e) {
            LOGGER.severe("Failed to create example event file: " + e.getMessage());
        }
    }
}