package com.eventui.fabric.adapter.signal;

import com.eventui.common.contract.signal.GameSignal;
import com.eventui.common.contract.signal.SignalBus;
import com.eventui.fabric.EventUIFabricMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

public class MinecraftSignalAdapter {
    private final SignalBus signalBus;

    public MinecraftSignalAdapter(SignalBus signalBus) {
        this.signalBus = signalBus;
    }

    public void registerListeners() {
        registerBlockBreakListener();

        EventUIFabricMod.LOGGER.info("Signal adapter listeners registered");
    }

    private void registerBlockBreakListener() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                String blockId = BuiltInRegistries.BLOCK
                        .getKey(state.getBlock())
                        .toString();

                String dimension = world.dimension().location().toString();

                GameSignal signal = new GameSignal.BlockBroken(
                        serverPlayer.getUUID(),
                        blockId,
                        dimension
                );

                signalBus.emit(signal);

                EventUIFabricMod.LOGGER.info(
                        "Block broken signal: {} broke {} in {}",
                        serverPlayer.getName().getString(),
                        blockId,
                        dimension
                );
            }
        });
    }
}

