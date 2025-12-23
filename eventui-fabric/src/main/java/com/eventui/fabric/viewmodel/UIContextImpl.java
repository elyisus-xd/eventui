package com.eventui.fabric.viewmodel;

import com.eventui.common.contract.ui.UIContext;
import com.eventui.common.dto.DataSnapshot;
import com.eventui.fabric.EventUIFabricMod;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Implementación del contrato UIContext.
 * Conecta la UI con el ViewModel.
 */
public class UIContextImpl implements UIContext {
    private final UUID playerId;
    private final MissionViewModel viewModel;

    public UIContextImpl(UUID playerId) {
        this.playerId = playerId;
        this.viewModel = new MissionViewModel(playerId);
    }

    @Override
    public void subscribe(String dataKey, Consumer<DataSnapshot> callback) {
        viewModel.subscribe(dataKey, callback);
    }

    @Override
    public void unsubscribe(String dataKey) {
        EventUIFabricMod.LOGGER.warn("Unsubscribe by dataKey not fully implemented yet");
    }

    @Override
    public DataSnapshot getData(String dataKey) {
        return viewModel.resolveDataKey(dataKey).orElse(null);
    }

    @Override
    public void sendAction(String actionId, Map<String, Object> params) {
        switch (actionId) {
            case "activate_mission" -> {
                String missionId = (String) params.get("missionId");
                if (missionId != null) {
                    var result = EventUIFabricMod.getCore()
                            .getCommandService()
                            .activateMission(playerId, missionId);

                    if (result.isSuccess()) {
                        EventUIFabricMod.LOGGER.info("Mission activated: {}", missionId);
                    } else {
                        EventUIFabricMod.LOGGER.error("Failed to activate mission: {}", result.getError());
                    }
                }
            }
            case "abandon_mission" -> {
                String missionId = (String) params.get("missionId");
                if (missionId != null) {
                    var result = EventUIFabricMod.getCore()
                            .getCommandService()
                            .abandonMission(playerId, missionId);

                    if (result.isSuccess()) {
                        EventUIFabricMod.LOGGER.info("Mission abandoned: {}", missionId);
                    } else {
                        EventUIFabricMod.LOGGER.error("Failed to abandon mission: {}", result.getError());
                    }
                }
            }
            default -> EventUIFabricMod.LOGGER.warn("Unknown action: {}", actionId);
        }
    }

    public void dispose() {
        viewModel.dispose();
    }
}
