package com.eventui.core.v2.config;

import com.eventui.api.ui.UIConfig;
import com.eventui.api.ui.UIElement;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Carga configuraciones de UI desde archivos YAML.
 * FASE 4A: Sistema declarativo de UI.
 */
public class UIConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(UIConfigLoader.class.getName());

    private final Yaml yaml;
    private final File uisDirectory;

    public UIConfigLoader(File pluginDataFolder) {
        this.yaml = new Yaml();
        this.uisDirectory = new File(pluginDataFolder, "uis");

        if (!uisDirectory.exists()) {
            uisDirectory.mkdirs();
            LOGGER.info("Created UIs directory at: " + uisDirectory.getAbsolutePath());
            createDefaultUIFile();
        }
    }

    /**
     * Carga todas las configuraciones de UI.
     */
    public Map<String, UIConfig> loadAllUIConfigs() {
        Map<String, UIConfig> configs = new HashMap<>();

        File[] files = uisDirectory.listFiles((dir, name) ->
                name.endsWith(".yml") || name.endsWith(".yaml"));

        if (files == null || files.length == 0) {
            LOGGER.warning("No UI config files found, using default UI");
            return configs;
        }

        for (File file : files) {
            try {
                UIConfig config = loadUIConfigFromFile(file);
                configs.put(config.getId(), config);
                LOGGER.info("✓ Loaded UI config: " + config.getId() + " from " + file.getName());

            } catch (Exception e) {
                LOGGER.severe("Failed to load UI config from " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return configs;
    }

    /**
     * Carga una UI config desde archivo.
     */
    @SuppressWarnings("unchecked")
    public UIConfig loadUIConfigFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Map<String, Object> data = yaml.load(fis);
            return parseUIConfig(data);
        }
    }

    /**
     * Parsea YAML a UIConfig.
     */
    @SuppressWarnings("unchecked")
    private UIConfig parseUIConfig(Map<String, Object> data) {
        String id = (String) data.get("id");
        String title = (String) data.getOrDefault("title", "EventUI");
        int screenWidth = ((Number) data.getOrDefault("screen_width", 320)).intValue();
        int screenHeight = ((Number) data.getOrDefault("screen_height", 240)).intValue();
        String associatedEventId = (String) data.get("associated_event_id");

        // Parsear screen properties
        Map<String, String> screenProperties = new HashMap<>();
        Map<String, Object> propsData = (Map<String, Object>) data.get("screen_properties");
        if (propsData != null) {
            propsData.forEach((key, value) -> screenProperties.put(key, value.toString()));
        }

        // Parsear elementos
        List<UIElement> elements = new ArrayList<>();
        List<Map<String, Object>> elementsData = (List<Map<String, Object>>) data.get("elements");

        if (elementsData != null) {
            for (Map<String, Object> elementData : elementsData) {
                elements.add(parseUIElement(elementData));
            }
        }

        return new com.eventui.core.v2.config.UIConfigImpl(
                id, title, screenWidth, screenHeight, elements, associatedEventId, screenProperties
        );
    }

    /**
     * Parsea un elemento UI desde YAML.
     */
    @SuppressWarnings("unchecked")
    private UIElement parseUIElement(Map<String, Object> data) {
        String id = (String) data.get("id");
        String typeStr = (String) data.get("type");
        com.eventui.api.ui.UIElementType type = com.eventui.api.ui.UIElementType.valueOf(typeStr.toUpperCase());

        int x = ((Number) data.get("x")).intValue();
        int y = ((Number) data.get("y")).intValue();
        int width = ((Number) data.getOrDefault("width", 100)).intValue();
        int height = ((Number) data.getOrDefault("height", 20)).intValue();
        int zIndex = ((Number) data.getOrDefault("z_index", 0)).intValue();
        boolean visible = (Boolean) data.getOrDefault("visible", true);

        // Parsear properties
        Map<String, String> properties = new HashMap<>();
        Map<String, Object> propsData = (Map<String, Object>) data.get("properties");
        if (propsData != null) {
            propsData.forEach((key, value) -> properties.put(key, value.toString()));
        }

        // Parsear children (elementos hijos)
        List<UIElement> children = new ArrayList<>();
        List<Map<String, Object>> childrenData = (List<Map<String, Object>>) data.get("children");
        if (childrenData != null) {
            for (Map<String, Object> childData : childrenData) {
                children.add(parseUIElement(childData));
            }
        }

        return new com.eventui.core.v2.config.UIElementImpl(
                id, type, x, y, width, height, properties, children, visible, zIndex
        );
    }

    /**
     * Crea un archivo de UI por defecto.
     */
    private void createDefaultUIFile() {
        File defaultFile = new File(uisDirectory, "default_event_list.yml");

        if (defaultFile.exists()) {
            return;
        }

        String defaultYaml = """
                id: default_event_list
                title: "Events"
                screen_width: 320
                screen_height: 240
                screen_properties:
                  blur_background: true
                  pause_game: false
                elements:
                  - id: title_text
                    type: TEXT
                    x: 160
                    y: 20
                    width: 200
                    height: 20
                    z_index: 10
                    properties:
                      content: "§6§lEVENTS"
                      align: "center"
                      shadow: true
                      
                  - id: close_button
                    type: BUTTON
                    x: 135
                    y: 210
                    width: 50
                    height: 20
                    z_index: 100
                    properties:
                      text: "Close"
                      action: "close_screen"
                """;

        try {
            java.nio.file.Files.writeString(defaultFile.toPath(), defaultYaml);
            LOGGER.info("Created default UI config file: " + defaultFile.getName());
        } catch (IOException e) {
            LOGGER.severe("Failed to create default UI file: " + e.getMessage());
        }
    }
}
