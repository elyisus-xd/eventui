package com.eventui.core.v2.commands;

import com.eventui.core.v2.EventUIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer para comandos de EventUI.
 * Proporciona sugerencias inteligentes basadas en el contexto.
 */
public class EventCommandTabCompleter implements TabCompleter {

    private final EventUIPlugin plugin;

    public EventCommandTabCompleter(EventUIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primer argumento: subcomandos
            List<String> subcommands = Arrays.asList(
                    "list", "info", "progress", "start", "reload",
                    "reset", "complete", "debug", "setprogress", "reloadevent"
            );

            String partial = args[0].toLowerCase();
            completions = subcommands.stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            // Segundo argumento: depende del subcomando
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "info", "start", "complete", "debug", "reloadevent" -> {
                    // Eventos disponibles
                    completions = getAvailableEventIds(args[1]);
                }
                case "progress" -> {
                    // Solo eventos que el jugador ha iniciado
                    if (sender instanceof Player player) {
                        completions = getPlayerEventIds(player, args[1]);
                    }
                }
                case "reset" -> {
                    // Eventos del jugador + "all"
                    completions = new ArrayList<>();
                    completions.add("all");
                    if (sender instanceof Player player) {
                        completions.addAll(getPlayerEventIds(player, args[1]));
                    }
                }
                case "setprogress" -> {
                    // Eventos en progreso
                    if (sender instanceof Player player) {
                        completions = getInProgressEventIds(player, args[1]);
                    }
                }
            }

        } else if (args.length == 3) {
            // Tercer argumento
            String subcommand = args[0].toLowerCase();

            if ("setprogress".equals(subcommand)) {
                // IDs de objetivos del evento seleccionado
                String eventId = args[1];
                completions = getObjectiveIds(eventId, args[2]);
            }

        } else if (args.length == 4) {
            // Cuarto argumento: cantidad (solo para setprogress)
            String subcommand = args[0].toLowerCase();

            if ("setprogress".equals(subcommand)) {
                // Sugerencias de cantidades comunes
                completions = Arrays.asList("0", "1", "5", "10", "50", "100");
            }
        }

        return completions;
    }

    /**
     * Obtiene IDs de todos los eventos disponibles.
     */
    private List<String> getAvailableEventIds(String partial) {
        return plugin.getStorage().getAllEventDefinitions().keySet().stream()
                .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene IDs de eventos que el jugador ha iniciado.
     */
    private List<String> getPlayerEventIds(Player player, String partial) {
        return plugin.getStorage().getAllEventDefinitions().keySet().stream()
                .filter(eventId -> {
                    var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
                    return progressOpt.isPresent();
                })
                .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene IDs de eventos que están IN_PROGRESS.
     */
    private List<String> getInProgressEventIds(Player player, String partial) {
        return plugin.getStorage().getAllEventDefinitions().keySet().stream()
                .filter(eventId -> {
                    var progressOpt = plugin.getStorage().getProgress(player.getUniqueId(), eventId);
                    return progressOpt.isPresent() &&
                            progressOpt.get().getState() == com.eventui.api.event.EventState.IN_PROGRESS;
                })
                .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene IDs de objetivos de un evento.
     */
    private List<String> getObjectiveIds(String eventId, String partial) {
        var eventOpt = plugin.getStorage().getEventDefinition(eventId);

        if (eventOpt.isEmpty()) {
            return List.of();
        }

        return eventOpt.get().getObjectives().stream()
                .map(obj -> obj.getId())
                .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}
