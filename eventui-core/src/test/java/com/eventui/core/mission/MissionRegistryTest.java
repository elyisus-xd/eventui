package com.eventui.core.mission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MissionRegistryTest {
    private MissionRegistry registry;
    private Mission testMission1;
    private Mission testMission2;

    @BeforeEach
    void setUp() {
        registry = new MissionRegistry();

        testMission1 = new Mission(
                "mission1",
                "Mission 1",
                "Description 1",
                List.of(),
                List.of(),
                List.of(),
                false,
                "combat",
                "easy",
                Map.of()
        );

        testMission2 = new Mission(
                "mission2",
                "Mission 2",
                "Description 2",
                List.of(),
                List.of(),
                List.of(),
                false,
                "exploration",
                "hard",
                Map.of()
        );
    }

    @Test
    void testRegister() {
        registry.register(testMission1);

        assertEquals(1, registry.size());
        assertTrue(registry.exists("mission1"));
    }

    @Test
    void testRegister_Duplicate() {
        registry.register(testMission1);

        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(testMission1);
        });
    }

    @Test
    void testGet_Exists() {
        registry.register(testMission1);

        Optional<Mission> result = registry.get("mission1");

        assertTrue(result.isPresent());
        assertEquals("mission1", result.get().getId());
    }

    @Test
    void testGet_NotExists() {
        Optional<Mission> result = registry.get("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAll() {
        registry.register(testMission1);
        registry.register(testMission2);

        var missions = registry.getAll();

        assertEquals(2, missions.size());
    }

    @Test
    void testGetByCategory() {
        registry.register(testMission1);
        registry.register(testMission2);

        var combatMissions = registry.getByCategory("combat");

        assertEquals(1, combatMissions.size());
        assertEquals("mission1", combatMissions.get(0).getId());
    }

    @Test
    void testExists() {
        registry.register(testMission1);

        assertTrue(registry.exists("mission1"));
        assertFalse(registry.exists("nonexistent"));
    }

    @Test
    void testSize() {
        assertEquals(0, registry.size());

        registry.register(testMission1);
        assertEquals(1, registry.size());

        registry.register(testMission2);
        assertEquals(2, registry.size());
    }

    @Test
    void testClear() {
        registry.register(testMission1);
        registry.register(testMission2);

        registry.clear();

        assertEquals(0, registry.size());
    }

    @Test
    void testUpdate() {
        registry.register(testMission1);

        Mission updated = new Mission(
                "mission1",
                "Updated Title",
                "Updated Description",
                List.of(),
                List.of(),
                List.of(),
                false,
                "combat",
                "easy",
                Map.of()
        );

        registry.update(updated);

        Optional<Mission> result = registry.get("mission1");
        assertTrue(result.isPresent());
        assertEquals("Updated Title", result.get().getTitle());
    }
}
