package com.eventui.core.v2.config;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveType;
import com.eventui.core.v2.event.EventDefinitionImpl;
import com.eventui.core.v2.objective.ObjectiveDefinitionImpl;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Carga definiciones de eventos desde archivos JSON.
 *
 * ARQUITECTURA:
 * - Lee archivos de plugins/EventUI/events/*.json
 * - Parsea y valida el JSON
 * - Crea instancias inmutables de EventDefinition
 * - Loguea errores de configuración sin crashear el plugin
 */
public class EventConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(EventConfigLoader.class.getName());
    private final Gson gson;
    private final File eventsDirectory;

    public EventConfigLoader(File pluginDataFolder) {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        this.eventsDirectory = new File(pluginDataFolder, "events");

        // Crear directorio si no existe
        if (!eventsDirectory.exists()) {
            eventsDirectory.mkdirs();
            LOGGER.info("Created events directory at: " + eventsDirectory.getAbsolutePath());
        }
    }

    /**
     * Carga todos los eventos del directorio.
     *
     * @return Mapa de ID → EventDefinition
     */
    public Map<String, EventDefinition> loadAllEvents() {
        Map<String, EventDefinition> events = new HashMap<>();

        File[] files = eventsDirectory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            LOGGER.warning("No event files found in: " + eventsDirectory.getAbsolutePath());
            createExampleEventFile();
            return events;
        }

        for (File file : files) {
            try {
                EventDefinition event = loadEventFromFile(file);
                events.put(event.getId(), event);
                LOGGER.info("Loaded event: " + event.getId() + " from " + file.getName());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load event from: " + file.getName(), e);
            }
        }

        return events;
    }

    /**
     * Carga un evento desde un archivo específico.
     */
    public EventDefinition loadEventFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            return parseEvent(json);
        }
    }

    /**
     * Parsea un JsonObject a EventDefinition.
     */
    private EventDefinition parseEvent(JsonObject json) {
        String id = json.get("id").getAsString();
        String displayName = json.get("display_name").getAsString();
        String description = json.get("description").getAsString();

        // Parsear objetivos
        List<ObjectiveDefinition> objectives = new ArrayList<>();
        JsonArray objectivesArray = json.getAsJsonArray("objectives");
        for (JsonElement element : objectivesArray) {
            objectives.add(parseObjective(element.getAsJsonObject()));
        }

        // Parsear UI resources
        Map<String, String> uiResources = parseStringMap(json, "ui_resources");

        // Parsear metadata
        Map<String, String> metadata = parseStringMap(json, "metadata");

        return new EventDefinitionImpl(
                id,
                displayName,
                description,
                objectives,
                uiResources,
                metadata
        );
    }

    /**
     * Parsea un objetivo desde JSON.
     */
    private ObjectiveDefinition parseObjective(JsonObject json) {
        String id = json.get("id").getAsString();
        ObjectiveType type = ObjectiveType.valueOf(json.get("type").getAsString());
        String description = json.get("description").getAsString();
        int targetAmount = json.get("target_amount").getAsInt();

        Map<String, String> parameters = parseStringMap(json, "parameters");
        Map<String, String> uiResources = parseStringMap(json, "ui_resources");

        boolean optional = json.has("optional") && json.get("optional").getAsBoolean();

        return new ObjectiveDefinitionImpl(
                id,
                type,
                description,
                targetAmount,
                parameters,
                uiResources,
                optional
        );
    }

    /**
     * Parsea un objeto JSON a Map<String, String>.
     */
    private Map<String, String> parseStringMap(JsonObject parent, String key) {
        Map<String, String> map = new HashMap<>();

        if (!parent.has(key)) {
            return map;
        }

        JsonObject obj = parent.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAsString());
        }

        return map;
    }

    /**
     * Crea un archivo de ejemplo si no existen eventos.
     */
    private void createExampleEventFile() {
        File exampleFile = new File(eventsDirectory, "tutorial_mining.json");

        if (exampleFile.exists()) {
            return;
        }

        String exampleJson = """
            {
              "id": "tutorial_mining",
              "display_name": "§6Tutorial de Minería",
              "description": "Aprende a minar tus primeros recursos",
              "objectives": [
                {
                  "id": "mine_stone",
                  "type": "MINE_BLOCK",
                  "description": "Mina 10 bloques de piedra",
                  "target_amount": 10,
                  "parameters": {
                    "block_id": "minecraft:stone"
                  }
                }
              ],
              "ui_resources": {
                "icon": "eventui:textures/events/mining_icon.png"
              },
              "metadata": {
                "category": "tutorial"
              }
            }
            """;

        try {
            java.nio.file.Files.writeString(exampleFile.toPath(), exampleJson);
            LOGGER.info("Created example event file: " + exampleFile.getName());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create example event file", e);
        }
    }
}
