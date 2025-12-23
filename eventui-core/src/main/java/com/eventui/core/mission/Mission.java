package com.eventui.core.mission;

import com.eventui.common.model.MissionState;
import java.util.List;
import java.util.Map;

/**
 * Entidad interna de misión.
 * Esta clase NO se expone fuera del Core, solo sus DTOs.
 */
public class Mission {
    private final String id;
    private final String title;
    private final String description;
    private final List<MissionObjective> objectives;
    private final List<String> prerequisites;
    private final List<MissionReward> rewards;
    private final boolean repeatable;
    private final String category;
    private final String difficulty;
    private final Map<String, Object> metadata;

    // Estado mutable (por jugador)
    private MissionState state;

    public Mission(
            String id,
            String title,
            String description,
            List<MissionObjective> objectives,
            List<String> prerequisites,
            List<MissionReward> rewards,
            boolean repeatable,
            String category,
            String difficulty,
            Map<String, Object> metadata
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.objectives = List.copyOf(objectives);
        this.prerequisites = List.copyOf(prerequisites);
        this.rewards = List.copyOf(rewards);
        this.repeatable = repeatable;
        this.category = category;
        this.difficulty = difficulty;
        this.metadata = Map.copyOf(metadata);
        this.state = MissionState.LOCKED; // Estado inicial por defecto
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<MissionObjective> getObjectives() { return objectives; }
    public List<String> getPrerequisites() { return prerequisites; }
    public List<MissionReward> getRewards() { return rewards; }
    public boolean isRepeatable() { return repeatable; }
    public String getCategory() { return category; }
    public String getDifficulty() { return difficulty; }
    public Map<String, Object> getMetadata() { return metadata; }
    public MissionState getState() { return state; }

    /**
     * Cambia el estado de la misión.
     * NOTA: Usar MissionStateMachine para cambios de estado validados.
     */
    public void setState(MissionState state) {
        this.state = state;
    }

    /**
     * @return true si la misión tiene prerequisitos
     */
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }

    /**
     * @return Objetivo principal (el primero)
     */
    public MissionObjective getPrimaryObjective() {
        return objectives.isEmpty() ? null : objectives.get(0);
    }

    /**
     * Crea una copia de esta misión (para instancias por jugador).
     */
    public Mission copy() {
        return new Mission(
                id, title, description, objectives, prerequisites,
                rewards, repeatable, category, difficulty, metadata
        );
    }
}
