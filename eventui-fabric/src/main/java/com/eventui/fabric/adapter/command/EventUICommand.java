package com.eventui.fabric.adapter.command;

import com.eventui.fabric.EventUIFabricMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EventUICommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("eventui")
                        .then(Commands.literal("stats")
                                .executes(EventUICommand::showStats)
                        )
                        .then(Commands.literal("missions")
                                .executes(EventUICommand::listMissions)
                        )
                        .then(Commands.literal("activate")
                                .executes(EventUICommand::activateMission)
                        )
        );
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        var stats = EventUIFabricMod.getCore().getStats();

        context.getSource().sendSuccess(
                () -> Component.literal("§6EventUI Statistics:"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§7Total Missions: §f" + stats.totalMissions()),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§7Loaded Players: §f" + stats.loadedPlayers()),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§7Event Listeners: §f" + stats.eventListeners()),
                false
        );

        return 1;
    }

    private static int listMissions(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be run by a player"));
            return 0;
        }

        var query = EventUIFabricMod.getCore().getQueryService();

        var available = query.getAvailableMissions(player.getUUID());
        var active = query.getActiveMissions(player.getUUID());
        var completed = query.getCompletedMissions(player.getUUID());

        context.getSource().sendSuccess(
                () -> Component.literal("§6Your Missions:"),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§aAvailable: §f" + available.size()),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§eActive: §f" + active.size()),
                false
        );
        context.getSource().sendSuccess(
                () -> Component.literal("§7Completed: §f" + completed.size()),
                false
        );

        return 1;
    }

    private static int activateMission(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command can only be run by a player"));
            return 0;
        }

        var query = EventUIFabricMod.getCore().getQueryService();
        var available = query.getAvailableMissions(player.getUUID());

        if (available.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo available missions to activate"));
            return 0;
        }

        var mission = available.get(0);
        var result = EventUIFabricMod.getCore()
                .getCommandService()
                .activateMission(player.getUUID(), mission.id());

        if (result.isSuccess()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§aMission activated: " + mission.title()),
                    false
            );
            return 1;
        } else {
            context.getSource().sendFailure(
                    Component.literal("§cFailed: " + result.getError())
            );
            return 0;
        }
    }
}
