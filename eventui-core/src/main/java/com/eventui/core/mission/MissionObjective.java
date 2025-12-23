package com.eventui.core.mission;

import com.eventui.common.contract.signal.GameSignal;

/**
 * Representa un objetivo de misión (ej: matar 10 zombies).
 */
public class MissionObjective {
    private final String id;
    private final ObjectiveType type;
    private final String target;
    private final int count;
    private final String dimension; // opcional, null = any dimension

    public MissionObjective(String id, ObjectiveType type, String target, int count, String dimension) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.count = count;
        this.dimension = dimension;
    }

    public String getId() { return id; }
    public ObjectiveType getType() { return type; }
    public String getTarget() { return target; }
    public int getCount() { return count; }
    public String getDimension() { return dimension; }

    /**
     * Verifica si una señal cumple con este objetivo.
     */
    public boolean matches(GameSignal signal) {
        return switch (type) {
            case KILL_ENTITY -> matchesEntityKill(signal);
            case CRAFT_ITEM -> matchesItemCraft(signal);
            case PLACE_BLOCK -> matchesBlockPlace(signal);
            case BREAK_BLOCK -> matchesBlockBreak(signal);
            case COLLECT_ITEM -> matchesItemPickup(signal);
        };
    }

    private boolean matchesEntityKill(GameSignal signal) {
        if (!(signal instanceof GameSignal.EntityKilled killed)) return false;
        if (!target.equals(killed.entityType())) return false;
        return dimension == null || dimension.equals(killed.dimension());
    }

    private boolean matchesItemCraft(GameSignal signal) {
        if (!(signal instanceof GameSignal.ItemCrafted crafted)) return false;
        return target.equals(crafted.itemId());
    }

    private boolean matchesBlockPlace(GameSignal signal) {
        if (!(signal instanceof GameSignal.BlockPlaced placed)) return false;
        if (!target.equals(placed.blockType())) return false;
        return dimension == null || dimension.equals(placed.dimension());
    }

    private boolean matchesBlockBreak(GameSignal signal) {
        if (!(signal instanceof GameSignal.BlockBroken broken)) return false;
        if (!target.equals(broken.blockType())) return false;
        return dimension == null || dimension.equals(broken.dimension());
    }

    private boolean matchesItemPickup(GameSignal signal) {
        if (!(signal instanceof GameSignal.ItemPickedUp pickup)) return false;
        return target.equals(pickup.itemId());
    }

    /**
     * Tipos de objetivos soportados.
     */
    public enum ObjectiveType {
        KILL_ENTITY,
        CRAFT_ITEM,
        PLACE_BLOCK,
        BREAK_BLOCK,
        COLLECT_ITEM
    }
}
