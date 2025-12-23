package com.eventui.core.mission;

/**
 * Representa una recompensa que se otorga al completar una misión.
 */
public class MissionReward {
    private final RewardType type;
    private final String item; // para ITEM
    private final int count;
    private final int experience; // para EXPERIENCE

    private MissionReward(RewardType type, String item, int count, int experience) {
        this.type = type;
        this.item = item;
        this.count = count;
        this.experience = experience;
    }

    public RewardType getType() { return type; }
    public String getItem() { return item; }
    public int getCount() { return count; }
    public int getExperience() { return experience; }

    /**
     * Factory methods para crear recompensas.
     */
    public static MissionReward item(String itemId, int count) {
        return new MissionReward(RewardType.ITEM, itemId, count, 0);
    }

    public static MissionReward experience(int amount) {
        return new MissionReward(RewardType.EXPERIENCE, null, 0, amount);
    }

    public enum RewardType {
        ITEM,
        EXPERIENCE
    }
}
