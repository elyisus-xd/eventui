package com.eventui.core;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.objective.ObjectiveType;
import com.eventui.api.ui.UIConfig;
import com.eventui.core.bridge.PluginEventBridge;
import com.eventui.core.commands.EventCommand;
import com.eventui.core.commands.EventCommandTabCompleter;
import com.eventui.core.config.EventConfigLoader;
import com.eventui.core.config.UIConfigLoader;
import com.eventui.core.rewards.RewardManager;
import com.eventui.core.storage.EventStorage;
import com.eventui.core.tracking.ObjectiveTracker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class EventUIPlugin extends JavaPlugin {

    private static EventUIPlugin instance;
    private static final Logger LOGGER = Logger.getLogger(EventUIPlugin.class.getName());

    private EventConfigLoader configLoader;
    private EventStorage storage;
    private PluginEventBridge eventBridge;
    private UIConfigLoader uiConfigLoader;
    private Map<String, UIConfig> uiConfigs;
    private RewardManager rewardManager;
    private ObjectiveTracker objectiveTracker;

    @Override
    public void onEnable() {
        instance = this;

        LOGGER.info("========================================");
        LOGGER.info("  EventUI Plugin initialization...");
        LOGGER.info("========================================");

        // Paso 1: Inicializar loader de configuración
        this.configLoader = new EventConfigLoader(getDataFolder());
        LOGGER.info("Initialized configuration loader");

        // Paso 1.5 - Inicializar loader de UIs
        this.uiConfigLoader = new UIConfigLoader(getDataFolder());
        this.uiConfigs = uiConfigLoader.loadAllUIConfigs();
        LOGGER.info("✓ Loaded " + uiConfigs.size() + " UI config(s)");

        // Paso 2: Inicializar storage
        this.storage = new EventStorage(this);
        LOGGER.info("Initialized event storage (in-memory)");

        this.rewardManager = new RewardManager(this);
        LOGGER.info("RewardManager initialized");

        // Paso 3: Cargar eventos desde JSON
        loadEvents();

        // Paso 4: Inicializar EventBridge
        initializeBridge();

        // Paso 5: Registrar tracker de objetivos
        registerTrackers();

        // ✅ NUEVO: Inicializar índices de optimización
        objectiveTracker.buildObjectiveTypeIndex();
        objectiveTracker.initializeActiveEventsIndex();
        LOGGER.info("✓ Initialized optimization indexes");

        // Paso 6: Registrar comandos
        registerCommands();
// ✅ Task OPTIMIZADO para COLLECT_ITEM (con índices)
        getServer().getScheduler().runTaskTimer(this, () ->
                getServer().getOnlinePlayers().forEach(player ->
                        objectiveTracker.checkCollectObjectives(player)
                ), 40L, 40L);
        // ✅ Task OPTIMIZADO para REACH_LOCATION (pre-filtrado + intervalo mayor)
        getServer().getScheduler().runTaskTimer(this, () -> {
            getServer().getOnlinePlayers().forEach(player -> {
                Set<String> relevantEvents = objectiveTracker.getRelevantActiveEvents(
                        player.getUniqueId(), ObjectiveType.REACH_LOCATION);

                if (!relevantEvents.isEmpty()) {
                    objectiveTracker.checkReachLocationObjectives(player);
                }
            });
        }, 40L, 40L);


        LOGGER.info("EventUI enabled successfully!");
        LOGGER.info("Loaded " + storage.getAllEventDefinitions().size() + " events");
    }


    public RewardManager getRewardManager() {
        return rewardManager;
    }

    @Override
    public void onDisable() {
        if (eventBridge != null) {
            eventBridge.getNetworkHandler().unregister();
        }

        LOGGER.info("EventUI disabled");
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
        // ✅ Crear y guardar la instancia
        this.objectiveTracker = new ObjectiveTracker(this);

        // Registrar como listener
        getServer().getPluginManager().registerEvents(objectiveTracker, this);

        LOGGER.info("Registered objective trackers");
    }


    private void registerCommands() {
        var command = getCommand("eventui");
        if (command != null) {
            EventCommand executor = new EventCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(new EventCommandTabCompleter(this));
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

    public ObjectiveTracker getObjectiveTracker() {
        return objectiveTracker;
    }

}
