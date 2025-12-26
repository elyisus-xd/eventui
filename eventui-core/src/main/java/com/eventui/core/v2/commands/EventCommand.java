package com.eventui.core.v2.commands;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.core.v2.EventUIPlugin;
import com.eventui.core.v2.event.EventProgressImpl;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Comando de testing para la FASE 1.
 * Comandos:
 * - /eventui list - Lista todos los eventos cargados
 * - /eventui info <id> - Muestra información de un evento
 * - /eventui progress <id> - Muestra tu progreso en un evento
 * - /eventui start <id> - Inicia un evento
 * - /eventui reload - Recarga eventos desde JSON
 */
public class EventCommand implements CommandExecutor {

    private static final Logger LOGGER = Logger.getLogger(EventCommand.class.getName());


    private final EventUIPlugin plugin;

    public EventCommand(EventUIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6EventUI v2 Commands:");
            sender.sendMessage("§e/eventui list §7- List all events");
            sender.sendMessage("§e/eventui info <id> §7- Show event info");
            sender.sendMessage("§e/eventui progress <id> §7- Show your progress");
            sender.sendMessage("§e/eventui start <id> §7- Start an event");
            sender.sendMessage("§e/eventui reload §7- Reload events");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "progress" -> handleProgress(sender, args);
            case "start" -> handleStart(sender, args);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage("§cUnknown command. Use /eventui for help");
        }

        return true;
    }

    private void handleList(CommandSender sender) {
        var events = plugin.getStorage().getAllEventDefinitions();

        if (events.isEmpty()) {
            sender.sendMessage("§cNo events loaded!");
            return;
        }

        sender.sendMessage("§6=== Loaded Events ===");
        events.values().forEach(event ->
                sender.sendMessage("§e" + event.getId() + " §7- §f" + event.getDisplayName())
        );
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eventui info <event_id>");
            return;
        }

        String eventId = args[1];
        Optional<EventDefinition> eventOpt = plugin.getStorage().getEventDefinition(eventId);

        if (eventOpt.isEmpty()) {
            sender.sendMessage("§cEvent not found: " + eventId);
            return;
        }

        EventDefinition event = eventOpt.get();
        sender.sendMessage("§6=== Event Info ===");
        sender.sendMessage("§eID: §f" + event.getId());
        sender.sendMessage("§eName: §f" + event.getDisplayName());
        sender.sendMessage("§eDescription: §f" + event.getDescription());
        sender.sendMessage("§eObjectives: §f" + event.getObjectives().size());

        event.getObjectives().forEach(obj ->
                sender.sendMessage("  §7- " + obj.getDescription() + " §8(" + obj.getTargetAmount() + ")")
        );
    }

    private void handleProgress(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can check progress!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eventui progress <event_id>");
            return;
        }

        String eventId = args[1];
        Optional<EventProgress> progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);

        if (progressOpt.isEmpty()) {
            sender.sendMessage("§cYou haven't started this event yet!");
            return;
        }

        EventProgress progress = progressOpt.get();
        sender.sendMessage("§6=== Your Progress ===");
        sender.sendMessage("§eEvent: §f" + eventId);
        sender.sendMessage("§eState: §f" + progress.getState());
        sender.sendMessage("§eOverall Progress: §f" + String.format("%.1f%%", progress.getOverallProgress() * 100));

        progress.getObjectivesProgress().forEach(obj ->
                sender.sendMessage("  §7- " + obj.getObjectiveId() + ": §f" +
                        obj.getCurrentAmount() + "/" + obj.getTargetAmount())
        );
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can start events!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eventui start <event_id>");
            return;
        }

        String eventId = args[1];

        try {
            // Verificar si el evento existe
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) {
                sender.sendMessage("§cEvent not found: " + eventId);
                return;
            }

            var eventDef = eventDefOpt.get();

            // Verificar progreso existente
            var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);

            if (progressOpt.isPresent()) {
                var progress = progressOpt.get();

                if (progress.getState() == com.eventui.api.event.EventState.COMPLETED) {
                    sender.sendMessage("§cYou have already completed this event!");
                    sender.sendMessage("§7This event cannot be repeated.");
                    return;
                }

                if (progress.getState() == com.eventui.api.event.EventState.IN_PROGRESS) {
                    sender.sendMessage("§eThis event is already in progress!");
                    sender.sendMessage("§7Use /eventui progress " + eventId + " to check your progress.");
                    return;
                }
            }

            // Iniciar el evento
            EventProgressImpl progress = plugin.getStorage().getOrCreateProgress(player.getUniqueId(), eventId);
            progress.start();

            sender.sendMessage("§aStarted event: " + eventDef.getDisplayName());

            // ✅ NUEVO: Notificar cambio de estado al cliente
            notifyStateChange(player.getUniqueId(), eventId, com.eventui.api.event.EventState.IN_PROGRESS);

            // ✅ NUEVO: Notificar progreso inicial (0/target) para que se muestre en la UI
            if (!eventDef.getObjectives().isEmpty()) {
                var firstObjective = eventDef.getObjectives().get(0);
                plugin.getEventBridge().notifyProgressUpdate(
                        player.getUniqueId(),
                        eventId,
                        firstObjective.getId(),
                        0,
                        firstObjective.getTargetAmount(),
                        firstObjective.getDescription()
                );
            }

        } catch (Exception e) {
            sender.sendMessage("§cFailed to start event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ NUEVO: Notifica cambio de estado al cliente.
     */
    private void notifyStateChange(UUID playerId, String eventId, com.eventui.api.event.EventState newState) {
        java.util.Map<String, String> payload = java.util.Map.of(
                "event_id", eventId,
                "new_state", newState.name()
        );

        com.eventui.api.bridge.BridgeMessage message = new com.eventui.core.v2.bridge.PluginBridgeMessage(
                com.eventui.api.bridge.MessageType.EVENT_STATE_CHANGED,
                payload,
                playerId
        );

        plugin.getEventBridge().sendMessage(message);

        LOGGER.info("Notified state change to client: event={}, newState={}");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("eventui.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return;
        }

        plugin.reloadEvents();
        sender.sendMessage("§aEvents reloaded successfully!");
    }

}
