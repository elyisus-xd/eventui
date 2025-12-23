package com.eventui.core.mission;

import com.eventui.common.contract.signal.GameSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MissionProgressTrackerTest {
    private MissionProgressTracker tracker;
    private UUID playerId;
    private Mission testMission;

    @BeforeEach
    void setUp() {
        tracker = new MissionProgressTracker();
        playerId = UUID.randomUUID();

        // Crear misión de prueba
        MissionObjective objective = new MissionObjective(
                "kill_zombies",
                MissionObjective.ObjectiveType.KILL_ENTITY,
                "minecraft:zombie",
                10,
                null
        );

        testMission = new Mission(
                "test_mission",
                "Test Mission",
                "Description",
                List.of(objective),
                List.of(),
                List.of(),
                false,
                "test",
                "easy",
                Map.of()
        );
    }

    @Test
    void testInitMission() {
        tracker.initMission(playerId, testMission);

        int progress = tracker.getProgress(playerId, "test_mission", "kill_zombies");
        assertEquals(0, progress);
    }

    @Test
    void testProcessSignal_Matching() {
        tracker.initMission(playerId, testMission);

        GameSignal signal = new GameSignal.EntityKilled(
                playerId,
                "minecraft:zombie",
                "minecraft:overworld"
        );

        int increment = tracker.processSignal(playerId, testMission, signal);

        assertEquals(1, increment);
        assertEquals(1, tracker.getProgress(playerId, "test_mission", "kill_zombies"));
    }

    @Test
    void testProcessSignal_NotMatching() {
        tracker.initMission(playerId, testMission);

        GameSignal signal = new GameSignal.EntityKilled(
                playerId,
                "minecraft:skeleton",
                "minecraft:overworld"
        );

        int increment = tracker.processSignal(playerId, testMission, signal);

        assertEquals(0, increment);
        assertEquals(0, tracker.getProgress(playerId, "test_mission", "kill_zombies"));
    }

    @Test
    void testProcessSignal_MultipleIncrements() {
        tracker.initMission(playerId, testMission);

        for (int i = 0; i < 5; i++) {
            GameSignal signal = new GameSignal.EntityKilled(
                    playerId,
                    "minecraft:zombie",
                    "minecraft:overworld"
            );
            tracker.processSignal(playerId, testMission, signal);
        }

        assertEquals(5, tracker.getProgress(playerId, "test_mission", "kill_zombies"));
    }

    @Test
    void testProcessSignal_CapsAtTarget() {
        tracker.initMission(playerId, testMission);

        // Enviar más señales que el target
        for (int i = 0; i < 15; i++) {
            GameSignal signal = new GameSignal.EntityKilled(
                    playerId,
                    "minecraft:zombie",
                    "minecraft:overworld"
            );
            tracker.processSignal(playerId, testMission, signal);
        }

        // No debe pasar de 10
        assertEquals(10, tracker.getProgress(playerId, "test_mission", "kill_zombies"));
    }

    @Test
    void testIsCompleted_NotCompleted() {
        tracker.initMission(playerId, testMission);

        assertFalse(tracker.isCompleted(playerId, testMission));
    }

    @Test
    void testIsCompleted_Completed() {
        tracker.initMission(playerId, testMission);

        // Completar el objetivo
        for (int i = 0; i < 10; i++) {
            GameSignal signal = new GameSignal.EntityKilled(
                    playerId,
                    "minecraft:zombie",
                    "minecraft:overworld"
            );
            tracker.processSignal(playerId, testMission, signal);
        }

        assertTrue(tracker.isCompleted(playerId, testMission));
    }

    @Test
    void testResetMission() {
        tracker.initMission(playerId, testMission);
        tracker.setProgress(playerId, "test_mission", "kill_zombies", 5);

        tracker.resetMission(playerId, "test_mission");

        assertEquals(0, tracker.getProgress(playerId, "test_mission", "kill_zombies"));
    }

    @Test
    void testSetProgress() {
        tracker.initMission(playerId, testMission);

        tracker.setProgress(playerId, "test_mission", "kill_zombies", 7);

        assertEquals(7, tracker.getProgress(playerId, "test_mission", "kill_zombies"));
    }
}
