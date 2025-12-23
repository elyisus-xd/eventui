package com.eventui.fabric.viewmodel;

import com.eventui.common.contract.mission.MissionEvent;
import com.eventui.common.contract.mission.MissionQuery;
import com.eventui.common.dto.DataSnapshot;
import com.eventui.common.dto.MissionDTO;
import com.eventui.common.dto.MissionProgressDTO;
import com.eventui.common.model.MissionState;
import com.eventui.fabric.EventUIFabricMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MissionViewModel {
    private final UUID playerId;
    private final MissionQuery queryService;

    private final Map<String, List<Consumer<DataSnapshot>>> subscribers;
    private final Map<String, DataSnapshot> cache;
    private Consumer<MissionEvent> eventListener;

    public MissionViewModel(UUID playerId) {
        this.playerId = playerId;
        this.queryService = EventUIFabricMod.getCore().getQueryService();
        this.subscribers = new ConcurrentHashMap<>();
        this.cache = new ConcurrentHashMap<>();

        subscribeToMissionEvents();
    }

    public void subscribe(String dataKey, Consumer<DataSnapshot> callback) {
        subscribers.computeIfAbsent(dataKey, k -> new ArrayList<>()).add(callback);
        resolveDataKey(dataKey).ifPresent(callback::accept);
    }

    public void unsubscribe(String dataKey, Consumer<DataSnapshot> callback) {
        List<Consumer<DataSnapshot>> callbacks = subscribers.get(dataKey);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    public Optional<DataSnapshot> resolveDataKey(String dataKey) {
        if (cache.containsKey(dataKey)) {
            return Optional.of(cache.get(dataKey));
        }

        DataSnapshot snapshot = switch (dataKey) {
            case "missions.available" -> createMissionsSnapshot(MissionState.AVAILABLE);
            case "missions.active" -> createMissionsSnapshot(MissionState.ACTIVE);
            case "missions.completed" -> createMissionsSnapshot(MissionState.COMPLETED);
            case "missions.locked" -> createMissionsSnapshot(MissionState.LOCKED);
            default -> {
                if (dataKey.startsWith("mission.") && dataKey.endsWith(".progress")) {
                    String missionId = dataKey.substring(8, dataKey.length() - 9);
                    yield createProgressSnapshot(missionId);
                }
                yield null;
            }
        };

        if (snapshot != null) {
            cache.put(dataKey, snapshot);
            return Optional.of(snapshot);
        }

        return Optional.empty();
    }

    private DataSnapshot createMissionsSnapshot(MissionState state) {
        List<MissionDTO> missions = queryService.getByState(playerId, state);

        List<Map<String, Object>> missionsList = new ArrayList<>();
        for (MissionDTO mission : missions) {
            Map<String, Object> missionData = new HashMap<>();
            missionData.put("id", mission.id());
            missionData.put("title", mission.title());
            missionData.put("description", mission.description());
            missionData.put("state", mission.state().name());
            missionData.put("progress", mission.progress());
            missionData.put("target", mission.target());
            missionData.put("percentage", mission.getProgressPercentage());
            missionData.put("category", mission.category());
            missionData.put("difficulty", mission.difficulty());
            missionData.put("repeatable", mission.isRepeatable());

            missionsList.add(missionData);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("missions", missionsList);
        data.put("count", missions.size());

        return new DataSnapshot("missions." + state.name().toLowerCase(), data, System.currentTimeMillis());
    }

    private DataSnapshot createProgressSnapshot(String missionId) {
        Optional<MissionProgressDTO> progressOpt = queryService.getProgress(playerId, missionId);

        if (progressOpt.isEmpty()) {
            return null;
        }

        MissionProgressDTO progress = progressOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("mission_id", progress.missionId());
        data.put("current", progress.current());
        data.put("target", progress.target());
        data.put("percentage", progress.percentage());

        return new DataSnapshot("mission." + missionId + ".progress", data, System.currentTimeMillis());
    }

    private void subscribeToMissionEvents() {
        this.eventListener = event -> {
            if (!event.playerId().equals(playerId)) {
                return;
            }

            if (event instanceof MissionEvent.ProgressChanged progressChanged) {
                invalidateCache("mission." + progressChanged.missionId() + ".progress");
                invalidateCache("missions.active");
            } else if (event instanceof MissionEvent.StateChanged stateChanged) {
                invalidateCache("missions.available");
                invalidateCache("missions.active");
                invalidateCache("missions.completed");
                invalidateCache("missions.locked");
            } else if (event instanceof MissionEvent.Unlocked unlocked) {
                invalidateCache("missions.available");
                invalidateCache("missions.locked");
            }
        };

        EventUIFabricMod.getCore().getMissionEventBus().subscribe(eventListener);
    }

    private void invalidateCache(String dataKey) {
        cache.remove(dataKey);

        List<Consumer<DataSnapshot>> callbacks = subscribers.get(dataKey);
        if (callbacks != null) {
            resolveDataKey(dataKey).ifPresent(snapshot -> {
                for (Consumer<DataSnapshot> callback : callbacks) {
                    try {
                        callback.accept(snapshot);
                    } catch (Exception e) {
                        EventUIFabricMod.LOGGER.error("Error notifying subscriber", e);
                    }
                }
            });
        }
    }

    public void dispose() {
        if (eventListener != null) {
            EventUIFabricMod.getCore().getMissionEventBus().unsubscribe(eventListener);
        }

        subscribers.clear();
        cache.clear();

        EventUIFabricMod.LOGGER.debug("ViewModel disposed for player: {}", playerId);
    }
}
