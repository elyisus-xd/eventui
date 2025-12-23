package com.eventui.core.command;

import com.eventui.common.contract.mission.MissionCommand;
import com.eventui.common.contract.mission.MissionEvent;
import com.eventui.common.contract.mission.MissionEventBus;
import com.eventui.common.dto.Result;
import com.eventui.common.model.MissionState;
import com.eventui.core.mission.Mission;
import com.eventui.core.mission.MissionProgressTracker;
import com.eventui.core.mission.MissionRegistry;
import com.eventui.core.mission.MissionStateMachine;
import com.eventui.core.player.PlayerMissionData;
import com.eventui.core.player.PlayerStateManager;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del servicio de comandos de misiones.
 * Maneja todas las operaciones de escritura y emite eventos correspondientes.
 */
public class MissionCommandService implements MissionCommand {
    private final MissionRegistry registry;
    private final PlayerStateManager stateManager;
    private final MissionStateMachine stateMachine;
    private final MissionProgressTracker progressTracker;
    private final MissionEventBus eventBus;

    public MissionCommandService(
            MissionRegistry registry,
            PlayerStateManager stateManager,
            MissionStateMachine stateMachine,
            MissionProgressTracker progressTracker,
            MissionEventBus eventBus
    ) {
        this.registry = registry;
        this.stateManager = stateManager;
        this.stateMachine = stateMachine;
        this.progressTracker = progressTracker;
        this.eventBus = eventBus;
    }

    @Override
    public Result<Void> activateMission(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        Optional<Mission> missionOpt = playerData.getMission(missionId);

        if (missionOpt.isEmpty()) {
            return new Result.Failure<>("Mission not found: " + missionId);
        }

        Mission mission = missionOpt.get();
        MissionState currentState = mission.getState();

        // Verificar transición válida
        if (!stateMachine.canActivate(currentState)) {
            return new Result.Failure<>("Cannot activate mission in state: " + currentState);
        }

        // Verificar prerequisites
        if (mission.hasPrerequisites()) {
            for (String prereqId : mission.getPrerequisites()) {
                Optional<Mission> prereq = playerData.getMission(prereqId);
                if (prereq.isEmpty() || prereq.get().getState() != MissionState.COMPLETED) {
                    return new Result.Failure<>("Prerequisite not completed: " + prereqId);
                }
            }
        }

        // Cambiar estado
        Result<MissionState> transitionResult = stateMachine.transition(
                currentState,
                MissionState.ACTIVE
        );

        if (transitionResult.isFailure()) {
            return new Result.Failure<>(transitionResult.getError());
        }

        // Actualizar estado
        playerData.setMissionState(missionId, MissionState.ACTIVE);

        // Inicializar progreso
        progressTracker.initMission(playerId, mission);

        // Emitir evento
        eventBus.publish(new MissionEvent.StateChanged(
                playerId,
                missionId,
                currentState,
                MissionState.ACTIVE
        ));

        return new Result.Success<>(null);
    }

    @Override
    public Result<Void> abandonMission(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        Optional<Mission> missionOpt = playerData.getMission(missionId);

        if (missionOpt.isEmpty()) {
            return new Result.Failure<>("Mission not found: " + missionId);
        }

        Mission mission = missionOpt.get();
        MissionState currentState = mission.getState();

        // Verificar que se puede abandonar
        if (!stateMachine.canAbandon(currentState)) {
            return new Result.Failure<>("Cannot abandon mission in state: " + currentState);
        }

        // Cambiar estado de vuelta a AVAILABLE
        playerData.setMissionState(missionId, MissionState.AVAILABLE);

        // Resetear progreso
        progressTracker.resetMission(playerId, missionId);

        // Emitir evento
        eventBus.publish(new MissionEvent.StateChanged(
                playerId,
                missionId,
                currentState,
                MissionState.AVAILABLE
        ));

        return new Result.Success<>(null);
    }

    @Override
    public Result<Void> completeMission(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        Optional<Mission> missionOpt = playerData.getMission(missionId);

        if (missionOpt.isEmpty()) {
            return new Result.Failure<>("Mission not found: " + missionId);
        }

        Mission mission = missionOpt.get();
        MissionState currentState = mission.getState();

        // Verificar transición
        Result<MissionState> transitionResult = stateMachine.transition(
                currentState,
                MissionState.COMPLETED
        );

        if (transitionResult.isFailure()) {
            return new Result.Failure<>(transitionResult.getError());
        }

        // Cambiar estado
        playerData.setMissionState(missionId, MissionState.COMPLETED);

        // Emitir evento de completado
        long completionTime = playerData.getCompletionTimestamp(missionId).orElse(System.currentTimeMillis());
        eventBus.publish(new MissionEvent.StateChanged(
                playerId,
                missionId,
                currentState,
                MissionState.COMPLETED
        ));

        // Desbloquear misiones que dependan de esta
        unlockDependentMissions(playerId, missionId);

        return new Result.Success<>(null);
    }

    @Override
    public Result<Void> resetMission(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        Optional<Mission> missionOpt = playerData.getMission(missionId);

        if (missionOpt.isEmpty()) {
            return new Result.Failure<>("Mission not found: " + missionId);
        }

        Mission mission = missionOpt.get();

        // Verificar que es repetible
        if (!mission.isRepeatable()) {
            return new Result.Failure<>("Mission is not repeatable: " + missionId);
        }

        // Verificar que está completada
        if (mission.getState() != MissionState.COMPLETED) {
            return new Result.Failure<>("Can only reset completed missions");
        }

        // Resetear a AVAILABLE
        playerData.setMissionState(missionId, MissionState.AVAILABLE);
        progressTracker.resetMission(playerId, missionId);

        // Emitir evento
        eventBus.publish(new MissionEvent.StateChanged(
                playerId,
                missionId,
                MissionState.COMPLETED,
                MissionState.AVAILABLE
        ));

        return new Result.Success<>(null);
    }

    /**
     * Desbloquea misiones que tenían como prerequisito la misión completada.
     */
    private void unlockDependentMissions(UUID playerId, String completedMissionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);

        // Buscar misiones bloqueadas que dependan de esta
        for (Mission mission : playerData.getAllMissions()) {
            if (mission.getState() == MissionState.LOCKED
                    && mission.getPrerequisites().contains(completedMissionId)) {

                // Verificar si todos los prerequisites están completados
                boolean allPrereqsComplete = mission.getPrerequisites().stream()
                        .allMatch(prereqId -> {
                            Optional<Mission> prereq = playerData.getMission(prereqId);
                            return prereq.isPresent() && prereq.get().getState() == MissionState.COMPLETED;
                        });

                if (allPrereqsComplete) {
                    playerData.setMissionState(mission.getId(), MissionState.AVAILABLE);

                    // Emitir evento de desbloqueo
                    eventBus.publish(new MissionEvent.Unlocked(playerId, mission.getId()));
                }
            }
        }
    }
}
