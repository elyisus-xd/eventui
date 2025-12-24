package com.eventui.core.v2;

import com.eventui.api.event.EventDefinition;
import com.eventui.core.v2.commands.EventCommand;
import com.eventui.core.v2.config.EventConfigLoader;
import com.eventui.core.v2.storage.EventStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Clase principal del plugin EventUI (Paper/Spigot).
 * ARQUITECTURA FASE 1:
 * - Carga eventos desde JSON
 * - Mantiene progreso en memoria
 * - Expone API para el MOD (vía EventBridge en siguiente paso)
 * - NO tiene comandos ni listeners todavía (solo infraestructura)
 */
public class EventUIPlugin extends JavaPlugin {

    private static EventUIPlugin instance;
    private static final Logger LOGGER = Logger.getLogger(EventUIPlugin.class.getName());

    private EventConfigLoader configLoader;
    private EventStorage storage;

    @Override
    public void onEnable() {
        instance = this;

        LOGGER.info("========================================");
        LOGGER.info("  EventUI v2 - Nueva Arquitectura");
        LOGGER.info("========================================");

        // Paso 1: Inicializar loader de configuración
        this.configLoader = new EventConfigLoader(getDataFolder());
        LOGGER.info("Initialized configuration loader");

        // Paso 2: Inicializar storage
        this.storage = new EventStorage();
        LOGGER.info("Initialized event storage (in-memory)");

        // Paso 3: Cargar eventos desde JSON
        loadEvents();

        // Paso 4: TODO - Inicializar EventBridge (próximo paso)

        LOGGER.info("EventUI v2 enabled successfully!");
        LOGGER.info("Loaded " + storage.getAllEventDefinitions().size() + " events");

        registerCommands();
    }

    @Override
    public void onDisable() {
        LOGGER.info("EventUI v2 disabled");
        instance = null;
    }

    /**
     * Carga todos los eventos desde archivos JSON.
     */
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

    /**
     * Recarga los eventos desde disco.
     */
    public void reloadEvents() {
        LOGGER.info("Reloading events...");
        loadEvents();
    }

    // ========== Getters públicos ==========

    public static EventUIPlugin getInstance() {
        return instance;
    }

    public EventStorage getStorage() {
        return storage;
    }

    public EventConfigLoader getConfigLoader() {
        return configLoader;
    }
    private void registerCommands() {
        getCommand("eventui").setExecutor(new EventCommand(this));
        LOGGER.info("Registered commands");
    }

}
