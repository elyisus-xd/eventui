package com.eventui.fabric;

import com.eventui.core.EventUICore;
import com.eventui.fabric.adapter.command.EventUICommand;
import com.eventui.fabric.adapter.signal.MinecraftSignalAdapter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EventUIFabricMod implements ModInitializer {
    public static final String MOD_ID = "eventui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static EventUICore core;
    private MinecraftSignalAdapter signalAdapter;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing EventUI mod...");

        // Inicializar el Core
        core = new EventUICore();

        // Cargar misiones
        loadMissions();

        // Inicializar adaptador de señales
        signalAdapter = new MinecraftSignalAdapter(core.getSignalBus());
        signalAdapter.registerListeners();

        // Registrar eventos de jugador
        registerPlayerEvents();

        // Registrar comandos
        registerCommands();

        LOGGER.info("EventUI initialized successfully!");
        LOGGER.info("Loaded {} missions", core.getMissionCount());
    }

    /**
     * Carga las misiones desde los archivos YAML en resources.
     */
    private void loadMissions() {
        List<InputStream> missionFiles = new ArrayList<>();

        try {
            InputStream tutorial = getClass().getResourceAsStream("/missions/tutorial.yml");
            InputStream combat = getClass().getResourceAsStream("/missions/combat_basics.yml");

            if (tutorial != null) missionFiles.add(tutorial);
            if (combat != null) missionFiles.add(combat);

            int loaded = core.loadMissions(missionFiles);
            LOGGER.info("Loaded {} mission definitions", loaded);

        } catch (Exception e) {
            LOGGER.error("Failed to load missions", e);
        }
    }

    /**
     * Registra eventos de conexión/desconexión de jugadores.
     */
    private void registerPlayerEvents() {
        // Cuando un jugador se conecta
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            core.initializePlayer(handler.player.getUUID());
            LOGGER.info("Initialized missions for player: {}", handler.player.getName().getString());
        });

        // Cuando un jugador se desconecta
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            core.cleanupPlayer(handler.player.getUUID());
            LOGGER.info("Cleaned up missions for player: {}", handler.player.getName().getString());
        });
    }

    /**
     * Registra comandos del mod.
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EventUICommand.register(dispatcher);
            LOGGER.info("EventUI commands registered");
        });
    }

    /**
     * Obtiene la instancia del Core.
     * @return Instancia singleton de EventUICore
     */
    public static EventUICore getCore() {
        if (core == null) {
            throw new IllegalStateException("EventUICore not initialized yet!");
        }
        return core;
    }
}
