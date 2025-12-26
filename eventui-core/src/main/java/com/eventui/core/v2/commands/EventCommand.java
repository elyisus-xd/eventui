package com.eventui.core.v2.commands;

import com.eventui.api.event.EventDefinition;
import com.eventui.api.event.EventProgress;
import com.eventui.core.v2.EventUIPlugin;
import com.eventui.core.v2.event.EventProgressImpl;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;
import java.util.Collection;

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
            sender.sendMessage("§e/ev list §7- List all events");
            sender.sendMessage("§e/ev info <id> §7- Show event info");
            sender.sendMessage("§e/ev progress <id> §7- Show your progress");
            sender.sendMessage("§e/ev start <id> §7- Start an event");
            sender.sendMessage("§e/ev reload §7- Reload all events");
            sender.sendMessage("§6§lTesting Commands:");
            sender.sendMessage("§e/ev reset <id|all> §7- Reset progress");
            sender.sendMessage("§e/ev complete <id> §7- Instant complete");
            sender.sendMessage("§e/ev debug <id> §7- Show debug info");
            sender.sendMessage("§e/ev setprogress <event> <obj> <amount> §7- Set progress");
            sender.sendMessage("§e/ev reloadevent <id> §7- Reload specific event");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "progress" -> handleProgress(sender, args);
            case "start" -> handleStart(sender, args);
            case "reload" -> handleReload(sender);
            case "reset" -> handleReset(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "debug" -> handleDebug(sender, args);
            case "setprogress" -> handleSetProgress(sender, args);     // ✅ NUEVO
            case "reloadevent" -> handleReloadEvent(sender, args);     // ✅ NUEVO
            default -> sender.sendMessage("§cUnknown command. Use /ev for help");
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

    /**
     * Reset de eventos para testing.
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can reset events!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eventui reset <event_id|all>");
            return;
        }

        String target = args[1];

        try {
            if (target.equalsIgnoreCase("all")) {
                // Reset TODOS los eventos
                var allEvents = plugin.getStorage().getAllEventDefinitions();
                int resetCount = 0;

                for (EventDefinition eventDef : allEvents.values()) {
                    var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventDef.getId());

                    if (progressOpt.isPresent()) {
                        // Eliminar progreso
                        plugin.getStorage().removeProgress(player.getUniqueId(), eventDef.getId());

                        // ✅ Notificar al cliente
                        notifyStateChange(player.getUniqueId(), eventDef.getId(), com.eventui.api.event.EventState.AVAILABLE);

                        resetCount++;
                    }
                }

                sender.sendMessage("§a✓ Reset completed!");
                sender.sendMessage("§7Cleared progress for " + resetCount + " event(s).");

                // ✅ IMPORTANTE: Solicitar refresh de la UI
                sender.sendMessage("§7Press K to refresh the events screen.");

            } else {
                // Reset un evento específico
                String eventId = target;

                var eventOpt = plugin.getStorage().getEventDefinition(eventId);
                if (eventOpt.isEmpty()) {
                    sender.sendMessage("§cEvent not found: " + eventId);
                    return;
                }

                var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
                if (progressOpt.isEmpty()) {
                    sender.sendMessage("§7You haven't started this event yet.");
                    return;
                }

                // Eliminar progreso
                plugin.getStorage().removeProgress(player.getUniqueId(), eventId);

                sender.sendMessage("§a✓ Event reset: " + eventOpt.get().getDisplayName());
                sender.sendMessage("§7Progress cleared. You can start it again.");

                // ✅ Notificar al cliente
                notifyStateChange(player.getUniqueId(), eventId, com.eventui.api.event.EventState.AVAILABLE);
            }

        } catch (Exception e) {
            sender.sendMessage("§cFailed to reset: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Completa instantáneamente un evento (para testing).
     */
    private void handleComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eventui complete <event_id>");
            return;
        }

        String eventId = args[1];

        try {
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) {
                sender.sendMessage("§cEvent not found: " + eventId);
                return;
            }

            var eventDef = eventDefOpt.get();

            // Crear/obtener progreso
            var progress = plugin.getStorage().getOrCreateProgress(player.getUniqueId(), eventId);

            // Si no está iniciado, iniciarlo primero
            if (progress.getState() == com.eventui.api.event.EventState.AVAILABLE) {
                progress.start();
            }

            // Completar todos los objetivos
            for (var objective : eventDef.getObjectives()) {
                var objProgress = progress.getObjectiveProgress(objective.getId());
                if (objProgress != null) {
                    objProgress.setProgress(objective.getTargetAmount());
                }
            }

            // Marcar como completado
            progress.complete();

            sender.sendMessage("§a✓ Event completed: " + eventDef.getDisplayName());

            // ✅ IMPORTANTE: Notificar al cliente el cambio de estado
            notifyStateChange(player.getUniqueId(), eventId, com.eventui.api.event.EventState.COMPLETED);

            // ✅ NUEVO: Enviar actualización de progreso final
            if (!eventDef.getObjectives().isEmpty()) {
                var lastObjective = eventDef.getObjectives().get(eventDef.getObjectives().size() - 1);
                plugin.getEventBridge().notifyProgressUpdate(
                        player.getUniqueId(),
                        eventId,
                        lastObjective.getId(),
                        lastObjective.getTargetAmount(),
                        lastObjective.getTargetAmount(),
                        lastObjective.getDescription()
                );
            }

        } catch (Exception e) {
            sender.sendMessage("§cFailed to complete: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Debug info detallada de un evento.
     */
    private void handleDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /eventui debug <event_id>");
            return;
        }

        String eventId = args[1];

        var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
        if (eventDefOpt.isEmpty()) {
            sender.sendMessage("§cEvent not found: " + eventId);
            return;
        }

        var eventDef = eventDefOpt.get();

        sender.sendMessage("§6═══ DEBUG INFO ═══");
        sender.sendMessage("§eEvent: §f" + eventDef.getDisplayName());
        sender.sendMessage("§eID: §7" + eventId);
        sender.sendMessage("§ePlayer: §7" + player.getName());

        var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);

        if (progressOpt.isEmpty()) {
            sender.sendMessage("§eState: §7AVAILABLE (not started)");
            sender.sendMessage("§eProgress: §70%");
        } else {
            var progress = progressOpt.get();
            sender.sendMessage("§eState: §f" + progress.getState());
            sender.sendMessage("§eOverall Progress: §a" + String.format("%.1f%%", progress.getOverallProgress() * 100));
            sender.sendMessage("§eStarted At: §7" + (progress.getStartedAt() > 0 ? new java.util.Date(progress.getStartedAt()) : "N/A"));

            sender.sendMessage("§eObjectives:");
            for (var objDef : eventDef.getObjectives()) {
                var objProgress = progress.getObjectivesProgress().stream()
                        .filter(op -> op.getObjectiveId().equals(objDef.getId()))
                        .findFirst()
                        .orElse(null);

                if (objProgress != null) {
                    String status = objProgress.isCompleted() ? "§a✓" : "§7○";
                    sender.sendMessage(String.format("  %s %s: §f%d/%d §7(%s)",
                            status,
                            objDef.getDescription(),
                            objProgress.getCurrentAmount(),
                            objProgress.getTargetAmount(),
                            objProgress.isCompleted() ? "COMPLETED" : "PENDING"
                    ));
                }
            }
        }

        sender.sendMessage("§6═══════════════");
    }
    /**
     * Establece el progreso de un objetivo específico.
     */
    private void handleSetProgress(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /ev setprogress <event_id> <objective_id> <amount>");
            return;
        }

        String eventId = args[1];
        String objectiveId = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number!");
            return;
        }

        try {
            // Verificar evento
            var eventDefOpt = plugin.getStorage().getEventDefinition(eventId);
            if (eventDefOpt.isEmpty()) {
                sender.sendMessage("§cEvent not found: " + eventId);
                return;
            }

            var eventDef = eventDefOpt.get();

            // Verificar objetivo
            var objectiveOpt = eventDef.getObjectives().stream()
                    .filter(obj -> obj.getId().equals(objectiveId))
                    .findFirst();

            if (objectiveOpt.isEmpty()) {
                sender.sendMessage("§cObjective not found: " + objectiveId);
                return;
            }

            var objective = objectiveOpt.get();

            // Obtener o crear progreso
            var progress = plugin.getStorage().getOrCreateProgress(player.getUniqueId(), eventId);

            // Si no está iniciado, iniciarlo
            if (progress.getState() == com.eventui.api.event.EventState.AVAILABLE) {
                progress.start();
                notifyStateChange(player.getUniqueId(), eventId, com.eventui.api.event.EventState.IN_PROGRESS);
            }

            // Establecer progreso del objetivo
            var objProgress = progress.getObjectiveProgress(objectiveId);
            if (objProgress != null) {
                objProgress.setProgress(amount);

                sender.sendMessage("§a✓ Progress updated!");
                sender.sendMessage(String.format("§7%s: §f%d/%d",
                        objective.getDescription(),
                        amount,
                        objective.getTargetAmount()));

                // Notificar al cliente
                plugin.getEventBridge().notifyProgressUpdate(
                        player.getUniqueId(),
                        eventId,
                        objectiveId,
                        amount,
                        objective.getTargetAmount(),
                        objective.getDescription()
                );

                // Verificar si se completó el evento
                if (progress.areAllObjectivesCompleted()) {
                    progress.complete();
                    sender.sendMessage("§6§l✓ EVENT COMPLETED!");
                    notifyStateChange(player.getUniqueId(), eventId, com.eventui.api.event.EventState.COMPLETED);
                }
            }

        } catch (Exception e) {
            sender.sendMessage("§cFailed to set progress: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Recarga un evento específico desde su archivo JSON.
     */
    private void handleReloadEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ev reloadevent <event_id>");
            return;
        }

        String eventId = args[1];

        try {
            // Verificar que el evento existe actualmente
            var currentEventOpt = plugin.getStorage().getEventDefinition(eventId);
            if (currentEventOpt.isEmpty()) {
                sender.sendMessage("§cEvent not found: " + eventId);
                sender.sendMessage("§7Use /ev reload to load all events.");
                return;
            }

            // Buscar el archivo JSON del evento
            java.io.File eventsDir = new java.io.File(plugin.getDataFolder(), "events");
            java.io.File[] files = eventsDir.listFiles((dir, name) -> {
                return name.endsWith(".json") && name.contains(eventId);
            });

            if (files == null || files.length == 0) {
                sender.sendMessage("§cCouldn't find JSON file for event: " + eventId);
                return;
            }

            // Cargar el archivo específico
            var newEventDef = plugin.getConfigLoader().loadEventFromFile(files[0]);

            // Actualizar en storage
            plugin.getStorage().registerEvent(newEventDef);

            sender.sendMessage("§a✓ Event reloaded: " + newEventDef.getDisplayName());
            sender.sendMessage("§7File: " + files[0].getName());
            sender.sendMessage("§7Objectives: " + newEventDef.getObjectives().size());

        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload event: " + e.getMessage());
            sender.sendMessage("§7Check console for details.");
            e.printStackTrace();
        }
    }





}
