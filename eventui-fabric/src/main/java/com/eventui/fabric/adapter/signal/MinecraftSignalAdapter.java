package com.eventui.fabric.adapter.signal;

import com.eventui.common.contract.signal.GameSignal;
import com.eventui.common.contract.signal.SignalBus;
import com.eventui.fabric.EventUIFabricMod;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/**
 * Adaptador que traduce eventos de Minecraft a GameSignals abstractas.
 */
public class MinecraftSignalAdapter {
    private final SignalBus signalBus;

    public MinecraftSignalAdapter(SignalBus signalBus) {
        this.signalBus = signalBus;
    }

    /**
     * Registra todos los listeners de eventos de Minecraft.
     */
    public void registerListeners() {
        registerBlockBreakListener();

        EventUIFabricMod.LOGGER.info("Signal adapter listeners registered");
    }

    /**
     * Listener para cuando un jugador rompe un bloque.
     */
    private void registerBlockBreakListener() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                // Obtener ID del bloque
                String blockId = BuiltInRegistries.BLOCK
                        .getKey(state.getBlock())
                        .toString();

                // Obtener dimensión
                String dimension = world.dimension().location().toString();

                // Traducir a GameSignal
                GameSignal signal = new GameSignal.BlockBroken(
                        serverPlayer.getUUID(),
                        blockId,
                        dimension
                );

                // Emitir señal al Core
                signalBus.emit(signal);

                EventUIFabricMod.LOGGER.debug(
                        "Block broken signal: {} broke {} in {}",
                        serverPlayer.getName().getString(),
                        blockId,
                        dimension
                );
            }
        });
    }
}
