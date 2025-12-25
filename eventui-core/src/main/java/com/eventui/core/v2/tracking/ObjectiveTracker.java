package com.eventui.core.v2.tracking;

import com.eventui.api.event.EventState;
import com.eventui.api.objective.ObjectiveDefinition;
import com.eventui.api.objective.ObjectiveType;
import com.eventui.core.v2.EventUIPlugin;
import com.eventui.core.v2.event.EventProgressImpl;
import com.eventui.core.v2.objective.ObjectiveProgressImpl;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.logging.Logger;

/**
 * Escucha eventos del juego y actualiza el progreso de objetivos.
 *
 * FASE 1: Solo implementa MINE_BLOCK
 */
public class ObjectiveTracker implements Listener {

    private static final Logger LOGGER = Logger.getLogger(ObjectiveTracker.class.getName());
    private final EventUIPlugin plugin;

    public ObjectiveTracker(EventUIPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Revisar todos los eventos activos del jugador
        plugin.getStorage().getAllEventDefinitions().values().forEach(eventDef -> {
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventDef.getId());

            if (progressOpt.isEmpty()) {
                return;
            }

            EventProgressImpl progress = (EventProgressImpl) progressOpt.get();

            if (progress.getState() != EventState.IN_PROGRESS) {
                return;
            }

            // Revisar objetivos de tipo MINE_BLOCK
            eventDef.getObjectives().forEach(objective -> {
                if (objective.getType() == ObjectiveType.MINE_BLOCK) {
                    String requiredBlock = objective.getParameters().get("block_id");

                    if (requiredBlock != null && blockType.getKey().toString().equals(requiredBlock)) {
                        ObjectiveProgressImpl objProgress = progress.getObjectiveProgress(objective.getId());

                        if (objProgress != null && !objProgress.isCompleted()) {
                            boolean completed = objProgress.increment(1);

                            player.sendMessage("§a[EventUI] Progreso: " +
                                    objProgress.getCurrentAmount() + "/" + objProgress.getTargetAmount() +
                                    " " + objective.getDescription());

                            // Notificar al MOD
                            plugin.getEventBridge().notifyProgressUpdate(
                                    player.getUniqueId(),
                                    eventDef.getId(),
                                    objective.getId(),
                                    objProgress.getCurrentAmount(),
                                    objProgress.getTargetAmount(),
                                    objective.getDescription()
                            );

                            if (completed) {
                                player.sendMessage("§6[EventUI] ¡Objetivo completado!");

                                if (progress.areAllObjectivesCompleted()) {
                                    progress.complete();
                                    player.sendMessage("§6§l[EventUI] ¡EVENTO COMPLETADO!");
                                }
                            }
                        }
                    }
                }
            });
        });
    }

}
