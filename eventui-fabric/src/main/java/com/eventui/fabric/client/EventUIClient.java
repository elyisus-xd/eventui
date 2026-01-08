package com.eventui.fabric.client;

import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.eventui.fabric.client.bridge.NetworkHandler;
import com.eventui.fabric.client.keybinds.EventUIKeybinds;
import com.eventui.fabric.client.ui.QuestTrackerHUD;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUIClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("EventUI-Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing EventUI client...");

        try {
            // Registrar payload type
            NetworkHandler.registerPayloadType();

            // Inicializar bridge
            ClientEventBridge.getInstance();

            // ✅ SIMPLIFICADO: Registrar keybinds (ahora en clase separada)
            LOGGER.info("Registering keybinds...");
            EventUIKeybinds.register();

            // ✅ Registrar Quest Tracker HUD
            LOGGER.info("Registering Quest Tracker HUD...");
            HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
                QuestTrackerHUD.render(graphics);
            });

        } catch (Exception e) {
            LOGGER.error("ERROR during initialization!", e);
        }

        LOGGER.info("EventUI client initialized!");
    }

    /**
     * FASE 4A: Abre una UI configurable (para testing).
     */
    public static void openConfigurableUI(com.eventui.api.ui.UIConfig config) {
        var client = net.minecraft.client.Minecraft.getInstance();
        client.setScreen(new com.eventui.fabric.client.ui.ConfigurableUIScreen(config));
    }
}
