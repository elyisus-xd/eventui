package com.eventui.core.v2.config;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveType;
import com.eventui.core.v2.event.EventDefinitionImpl;
import com.eventui.core.v2.objective.ObjectiveDefinitionImpl;
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
     * Carga todos los eventos del directorio.
     */
    public Map<String, EventDefinition> loadAllEvents() {
        Map<String, EventDefinition> events = new HashMap<>();

        File[] files = eventsDirectory.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml"));

        if (files == null || files.length == 0) {
            LOGGER.warning("No event files found in: " + eventsDirectory.getAbsolutePath());
            createExampleEventFile();
            return events;
        }

        for (File file : files) {
            try {
                EventDefinition event = loadEventFromFile(file);
                events.put(event.getId(), event);
                LOGGER.info("✓ Loaded event: " + event.getId() + " from " + file.getName());

            } catch (Exception e) {
                LOGGER.severe("═══════════════════════════════");
                LOGGER.severe("YAML ERROR in file: " + file.getName());
                LOGGER.severe("Problem: " + e.getMessage());
                LOGGER.severe("Common fixes:");
                LOGGER.severe("  - Check indentation (use spaces, not tabs)");
                LOGGER.severe("  - Verify all strings are properly quoted");
                LOGGER.severe("  - Use YAML validator: http://www.yamllint.com");
                LOGGER.severe("File location: " + file.getAbsolutePath());
                LOGGER.severe("═══════════════════════════════");
            }
        }

        if (events.isEmpty()) {
            LOGGER.warning("No events were loaded successfully. Check the errors above.");
        } else {
            LOGGER.info("Successfully loaded " + events.size() + " event(s)");
        }

        return events;
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
        String displayName = (String) data.get("display_name");
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

        return new EventDefinitionImpl(id, displayName, description, objectives, uiResources, metadata);
    }

    /**
     * Parsea un objetivo desde YAML.
     */
    @SuppressWarnings("unchecked")
    private ObjectiveDefinition parseObjective(Map<String, Object> data) {
        String id = (String) data.get("id");
        ObjectiveType type = ObjectiveType.valueOf(((String) data.get("type")).toUpperCase());
        String description = (String) data.get("description");
        int targetAmount = ((Number) data.get("target_amount")).intValue();

        // Parsear parameters
        Map<String, String> parameters = new HashMap<>();
        Map<String, Object> parametersData = (Map<String, Object>) data.get("parameters");
        if (parametersData != null) {
            parametersData.forEach((key, value) -> parameters.put(key, value.toString()));
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
                id: tutorial_mining
                display_name: "§6Tutorial de Minería"
                description: "Aprende a minar tus primeros recursos"
                objectives:
                  - id: mine_stone
                    type: MINE_BLOCK
                    description: "Mina 10 bloques de piedra"
                    target_amount: 10
                    parameters:
                      block_id: "minecraft:stone"
                ui_resources:
                  icon: "eventui:textures/events/mining_icon.png"
                metadata:
                  category: "tutorial"
                  repeatable: false
                """;

        try {
            java.nio.file.Files.writeString(exampleFile.toPath(), exampleYaml);
            LOGGER.info("Created example event file: " + exampleFile.getName());
        } catch (IOException e) {
            LOGGER.severe("Failed to create example event file: " + e.getMessage());
        }
    }
}
