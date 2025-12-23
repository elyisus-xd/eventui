package com.eventui.core.query;

import com.eventui.common.contract.mission.MissionQuery;
import com.eventui.common.dto.MissionDTO;
import com.eventui.common.dto.MissionProgressDTO;
import com.eventui.common.model.MissionState;
import com.eventui.core.mission.Mission;
import com.eventui.core.mission.MissionProgressTracker;
import com.eventui.core.mission.MissionRegistry;
import com.eventui.core.player.PlayerMissionData;
import com.eventui.core.player.PlayerStateManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MissionQueryService implements MissionQuery {
    private final MissionRegistry registry;
    private final PlayerStateManager stateManager;
    private final MissionProgressTracker progressTracker;

    public MissionQueryService(
            MissionRegistry registry,
            PlayerStateManager stateManager,
            MissionProgressTracker progressTracker
    ) {
        this.registry = registry;
        this.stateManager = stateManager;
        this.progressTracker = progressTracker;
    }

    @Override
    public List<MissionDTO> getByState(UUID playerId, MissionState state) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        return playerData.getMissionsByState(state).stream()
                .map(mission -> toDTO(playerId, mission))
                .toList();
    }

    @Override
    public Optional<MissionDTO> getById(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        return playerData.getMission(missionId)
                .map(mission -> toDTO(playerId, mission));
    }

    @Override
    public Optional<MissionProgressDTO> getProgress(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        return playerData.getMission(missionId)
                .filter(m -> m.getState() == MissionState.ACTIVE)
                .map(mission -> {
                    var objective = mission.getPrimaryObjective();
                    if (objective == null) return null;

                    int current = progressTracker.getProgress(
                            playerId,
                            missionId,
                            objective.getId()
                    );

                    return new MissionProgressDTO(
                            missionId,
                            current,
                            objective.getCount()
                    );
                });
    }

    @Override
    public List<MissionDTO> getAvailableMissions(UUID playerId) {
        return getByState(playerId, MissionState.AVAILABLE);
    }

    @Override
    public List<MissionDTO> getActiveMissions(UUID playerId) {
        return getByState(playerId, MissionState.ACTIVE);
    }

    @Override
    public List<MissionDTO> getCompletedMissions(UUID playerId) {
        return getByState(playerId, MissionState.COMPLETED);
    }

    @Override
    public boolean canActivate(UUID playerId, String missionId) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        Optional<Mission> missionOpt = playerData.getMission(missionId);

        if (missionOpt.isEmpty()) return false;
        Mission mission = missionOpt.get();

        if (mission.getState() != MissionState.AVAILABLE) return false;

        if (mission.hasPrerequisites()) {
            for (String prereqId : mission.getPrerequisites()) {
                Optional<Mission> prereq = playerData.getMission(prereqId);
                if (prereq.isEmpty() || prereq.get().getState() != MissionState.COMPLETED) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<MissionDTO> getByCategory(UUID playerId, String category) {
        PlayerMissionData playerData = stateManager.getOrCreate(playerId);
        return playerData.getAllMissions().stream()
                .filter(m -> category.equals(m.getCategory()))
                .map(mission -> toDTO(playerId, mission))
                .toList();
    }

    /**
     * Convierte una entidad Mission interna a DTO público CON PROGRESO.
     */
    private MissionDTO toDTO(UUID playerId, Mission mission) {
        var objective = mission.getPrimaryObjective();
        int progress = 0;
        int target = 0;

        if (objective != null) {
            target = objective.getCount();
            // Obtener progreso si la misión está activa
            if (mission.getState() == MissionState.ACTIVE) {
                progress = progressTracker.getProgress(playerId, mission.getId(), objective.getId());
            }
        }

        return new MissionDTO(
                mission.getId(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getState(),
                progress,
                target,
                mission.getPrerequisites(),
                mission.isRepeatable(),
                mission.getCategory(),
                mission.getDifficulty()
        );
    }
}
