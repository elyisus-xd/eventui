package com.eventui.core.v2;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.ui.UIConfig;
import com.eventui.core.v2.bridge.PluginEventBridge;
import com.eventui.core.v2.commands.EventCommand;
import com.eventui.core.v2.config.EventConfigLoader;
import com.eventui.core.v2.config.UIConfigLoader;
import com.eventui.core.v2.storage.EventStorage;
import com.eventui.core.v2.tracking.ObjectiveTracker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.logging.Logger;

public class EventUIPlugin extends JavaPlugin {

    private static EventUIPlugin instance;
    private static final Logger LOGGER = Logger.getLogger(EventUIPlugin.class.getName());

    private EventConfigLoader configLoader;
    private EventStorage storage;
    private PluginEventBridge eventBridge;
    private UIConfigLoader uiConfigLoader;
    private Map<String, UIConfig> uiConfigs;

    @Override
    public void onEnable() {
        instance = this;

        LOGGER.info("========================================");
        LOGGER.info("  EventUI v2 - Nueva Arquitectura");
        LOGGER.info("========================================");

        // Paso 1: Inicializar loader de configuración
        this.configLoader = new EventConfigLoader(getDataFolder());
        LOGGER.info("Initialized configuration loader");

        // ✅ NUEVO: Paso 1.5 - Inicializar loader de UIs
        this.uiConfigLoader = new com.eventui.core.v2.config.UIConfigLoader(getDataFolder());
        this.uiConfigs = uiConfigLoader.loadAllUIConfigs();
        LOGGER.info("✓ Loaded " + uiConfigs.size() + " UI config(s)");

        // Paso 2: Inicializar storage
        this.storage = new EventStorage();
        LOGGER.info("Initialized event storage (in-memory)");

        // Paso 3: Cargar eventos desde JSON
        loadEvents();

        // Paso 4: Inicializar EventBridge
        initializeBridge();

        // Paso 5: Registrar tracker de objetivos
        registerTrackers();

        // Paso 6: Registrar comandos
        registerCommands();

        LOGGER.info("EventUI v2 enabled successfully!");
        LOGGER.info("Loaded " + storage.getAllEventDefinitions().size() + " events");
    }

    @Override
    public void onDisable() {
        if (eventBridge != null) {
            eventBridge.getNetworkHandler().unregister();
        }

        LOGGER.info("EventUI v2 disabled");
        instance = null;
    }

    private void loadEvents() {
        try {
            Map<String, EventDefinition> events = configLoader.loadAllEvents();

            if (events.isEmpty()) {
                LOGGER.warning("No events loaded! Check your events/ directory");
                return;
            }

            storage.registerEvents(events);

            LOGGER.info("Successfully loaded events:");
            events.values().forEach(event ->
                    LOGGER.info("  - " + event.getId() + ": " + event.getDisplayName())
            );

        } catch (Exception e) {
            LOGGER.severe("Failed to load events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeBridge() {
        this.eventBridge = new PluginEventBridge(this);
        LOGGER.info("EventBridge initialized");
    }

    private void registerTrackers() {
        getServer().getPluginManager().registerEvents(new ObjectiveTracker(this), this);
        LOGGER.info("Registered objective trackers");
    }

    private void registerCommands() {
        var command = getCommand("eventui");
        if (command != null) {
            EventCommand executor = new com.eventui.core.v2.commands.EventCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(new com.eventui.core.v2.commands.EventCommandTabCompleter(this));
            LOGGER.info("Registered commands with tab completion");
        } else {
            LOGGER.warning("Failed to register eventui command - command not found in plugin.yml!");
        }
    }



    public void reloadEvents() {
        LOGGER.info("Reloading events...");
        loadEvents();
    }
    // ✅ NUEVO: Getter para UI configs
    public Map<String, UIConfig> getUIConfigs() {
        return uiConfigs;
    }

    public UIConfigLoader getUIConfigLoader() {
        return uiConfigLoader;
    }

    public static EventUIPlugin getInstance() {
        return instance;
    }

    public EventStorage getStorage() {
        return storage;
    }

    public EventConfigLoader getConfigLoader() {
        return configLoader;
    }

    public PluginEventBridge getEventBridge() {
        return eventBridge;
    }
}
