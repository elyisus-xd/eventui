package com.eventui.core;

import com.eventui.common.contract.mission.*;
import com.eventui.common.contract.signal.GameSignal;
import com.eventui.common.contract.signal.SignalBus;
import com.eventui.common.model.MissionState;
import com.eventui.core.command.MissionCommandService;
import com.eventui.core.event.EventBusImpl;
import com.eventui.core.loader.MissionDefinitionLoader;
import com.eventui.core.mission.*;
import com.eventui.core.player.PlayerMissionData;
import com.eventui.core.player.PlayerStateManager;
import com.eventui.core.query.MissionQueryService;
import com.eventui.core.signal.SignalBusImpl;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Fachada principal del Core.
 * Punto de entrada único para interactuar con el sistema de misiones.
 * Esta clase orquesta todos los componentes internos y expone
 * solo las interfaces del contrato.
 */
public class EventUICore {
    // Registros y managers
    private final MissionRegistry registry;
    private final PlayerStateManager stateManager;
    private final MissionProgressTracker progressTracker;

    // State machine
    private final MissionStateMachine stateMachine;

    // Event buses
    private final MissionEventBus missionEventBus;
    private final SignalBus signalBus;

    // Services
    private final MissionQueryService queryService;
    private final MissionCommandService commandService;

    // Loader
    private final MissionDefinitionLoader loader;

    public EventUICore() {
        // Inicializar componentes
        this.registry = new MissionRegistry();
        this.stateManager = new PlayerStateManager();
        this.progressTracker = new MissionProgressTracker();
        this.stateMachine = new MissionStateMachine();

        // Event buses
        this.missionEventBus = new EventBusImpl();
        this.signalBus = new SignalBusImpl();

        // Services
        this.queryService = new MissionQueryService(registry, stateManager, progressTracker);
        this.commandService = new MissionCommandService(
                registry,
                stateManager,
                stateMachine,
                progressTracker,
                missionEventBus
        );

        // Loader
        this.loader = new MissionDefinitionLoader();

        // Conectar signal bus al sistema de misiones
        setupSignalHandlers();
    }

    // ==================== API PÚBLICA ====================

    /**
     * Obtiene el servicio de queries (solo lectura).
     */
    public MissionQuery getQueryService() {
        return queryService;
    }

    /**
     * Obtiene el servicio de comandos (escritura).
     */
    public MissionCommand getCommandService() {
        return commandService;
    }

    /**
     * Obtiene el bus de eventos de misiones.
     */
    public MissionEventBus getMissionEventBus() {
        return missionEventBus;
    }

    /**
     * Obtiene el bus de señales del juego.
     */
    public SignalBus getSignalBus() {
        return signalBus;
    }

    // ==================== GESTIÓN DE MISIONES ====================

    /**
     * Carga misiones desde archivos YAML.
     *
     * @param inputs Streams de archivos YAML
     * @return Cantidad de misiones cargadas exitosamente
     */
    public int loadMissions(List<InputStream> inputs) {
        List<Mission> missions = loader.loadAll(inputs);
        for (Mission mission : missions) {
            registry.register(mission);
        }
        return missions.size();
    }

    /**
     * Inicializa un jugador en el sistema.
     * Debe llamarse cuando un jugador se conecta.
     *
     * @param playerId ID del jugador
     */
    public void initializePlayer(UUID playerId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);

        // Inicializar todas las misiones del registro para este jugador
        for (Mission template : registry.getAll()) {
            playerData.initMission(template);

            // Si no tiene prerequisites, está disponible desde el inicio
            if (!template.hasPrerequisites()) {
                playerData.setMissionState(template.getId(), MissionState.AVAILABLE);
            }
        }
    }

    /**
     * Limpia los datos de un jugador cuando se desconecta.
     *
     * @param playerId ID del jugador
     */
    public void cleanupPlayer(UUID playerId) {
        stateManager.remove(playerId);
    }

    // ==================== PROCESAMIENTO DE SEÑALES ====================

    /**
     * Configura los handlers de señales para actualizar progreso de misiones.
     */
    private void setupSignalHandlers() {
        signalBus.subscribeAll(signal -> processSignalForAllPlayers(signal));
    }

    /**
     * Procesa una señal para todas las misiones activas de un jugador.
     */
    private void processSignalForAllPlayers(GameSignal signal) {
        UUID playerId = signal.playerId();

        PlayerMissionData playerData = stateManager.get(playerId).orElse(null);
        if (playerData == null) return;

        // Procesar todas las misiones activas
        for (Mission mission : playerData.getMissionsByState(MissionState.ACTIVE)) {
            int oldProgress = 0;
            var objective = mission.getPrimaryObjective();
            if (objective != null) {
                oldProgress = progressTracker.getProgress(playerId, mission.getId(), objective.getId());
            }

            // Intentar actualizar progreso
            int increment = progressTracker.processSignal(playerId, mission, signal);

            if (increment > 0) {
                int newProgress = oldProgress + increment;
                int target = objective != null ? objective.getCount() : 0;

                // Emitir evento de progreso
                missionEventBus.publish(new MissionEvent.ProgressChanged(
                        playerId,
                        mission.getId(),
                        oldProgress,
                        newProgress,
                        target
                ));

                // Verificar si se completó
                if (progressTracker.isCompleted(playerId, mission)) {
                    commandService.completeMission(playerId, mission.getId());
                }
            }
        }
    }

    // ==================== INFORMACIÓN DEL SISTEMA ====================

    /**
     * @return Cantidad de misiones registradas
     */
    public int getMissionCount() {
        return registry.size();
    }

    /**
     * @return Cantidad de jugadores con datos cargados
     */
    public int getLoadedPlayerCount() {
        return stateManager.getLoadedPlayerCount();
    }

    /**
     * Obtiene estadísticas del sistema.
     */
    public SystemStats getStats() {
        return new SystemStats(
                registry.size(),
                stateManager.getLoadedPlayerCount(),
                ((EventBusImpl) missionEventBus).getListenerCount()
        );
    }

    /**
     * Record con estadísticas del sistema.
     */
    public record SystemStats(
            int totalMissions,
            int loadedPlayers,
            int eventListeners
    ) {}
}

