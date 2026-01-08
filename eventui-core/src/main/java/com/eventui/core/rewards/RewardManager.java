package com.eventui.core.rewards;

import com.eventui.api.event.EventDefinition;
import com.eventui.core.EventUIPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gestiona el otorgamiento de recompensas a los jugadores.
 */
public class RewardManager {

    private static final Logger LOGGER = Logger.getLogger(RewardManager.class.getName());

    private final EventUIPlugin plugin;

    public RewardManager(EventUIPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Otorga las recompensas de un evento al jugador.
     */
    public void giveRewards(Player player, EventDefinition eventDef) {
        try {
            // Parsear rewards desde metadata
            String rewardsJson = eventDef.getMetadata().getOrDefault("rewards_data", "{}");

            if (rewardsJson.equals("{}")) {
                LOGGER.info("No rewards configured for event: " + eventDef.getId());
                return;
            }

            com.google.gson.Gson gson = new com.google.gson.Gson();
            Map<String, Object> rewards = gson.fromJson(rewardsJson,
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());

            int rewardCount = 0;

            // Otorgar XP
            if (rewards.containsKey("xp")) {
                int xp = ((Number) rewards.get("xp")).intValue();
                player.giveExp(xp);
                player.sendMessage("§a+ " + xp + " XP");
                rewardCount++;
            }

            // Otorgar items
            if (rewards.containsKey("items")) {
                List<String> items = (List<String>) rewards.get("items");

                for (String itemString : items) {
                    try {
                        ItemStack item = parseItemString(itemString);

                        if (item != null) {
                            // Intentar agregar al inventario
                            var leftover = player.getInventory().addItem(item);

                            if (!leftover.isEmpty()) {
                                // Si el inventario está lleno, tirar al suelo
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                                player.sendMessage("§e⚠ Your inventory is full! Item dropped on the ground.");
                            }

                            player.sendMessage("§a+ " + item.getAmount() + "x " +
                                    item.getType().name().toLowerCase().replace("_", " "));
                            rewardCount++;
                        }

                    } catch (Exception e) {
                        LOGGER.warning("Failed to parse item: " + itemString);
                    }
                }
            }

            // Ejecutar comandos
            if (rewards.containsKey("commands")) {
                List<String> commands = (List<String>) rewards.get("commands");

                for (String command : commands) {
                    String processedCommand = command.replace("{player}", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
                    rewardCount++;
                }
            }

            if (rewardCount > 0) {
                player.sendMessage("§6✓ Received " + rewardCount + " reward(s)!");
                LOGGER.info("Gave " + rewardCount + " reward(s) to " + player.getName() +
                        " for completing event: " + eventDef.getId());
            }

        } catch (Exception e) {
            LOGGER.severe("Failed to give rewards for event " + eventDef.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parsea un string "minecraft:item_id cantidad" a ItemStack.
     * Formato esperado: "minecraft:diamond_pickaxe 1"
     */
    private ItemStack parseItemString(String itemString) {
        String[] parts = itemString.trim().split(" ");

        if (parts.length < 1) {
            return null;
        }

        String itemId = parts[0];
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

        // Convertir "minecraft:diamond_pickaxe" a "DIAMOND_PICKAXE"
        String materialName = itemId.replace("minecraft:", "").toUpperCase();

        try {
            Material material = Material.valueOf(materialName);
            return new ItemStack(material, amount);

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown material: " + materialName);
            return null;
        }
    }
}
