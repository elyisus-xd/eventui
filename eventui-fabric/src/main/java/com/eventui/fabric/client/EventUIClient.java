package com.eventui.fabric.client;

import com.eventui.fabric.EventUIFabricMod;
import com.eventui.fabric.ui.screen.MissionListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class EventUIClient implements ClientModInitializer {
    private static KeyMapping openMissionsKey;

    @Override
    public void onInitializeClient() {
        EventUIFabricMod.LOGGER.info("EventUI client initialized!");

        // Registrar keybinding
        registerKeyBindings();

        // Registrar listener de teclas
        registerKeyHandlers();
    }

    private void registerKeyBindings() {
        openMissionsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.eventui.open_missions",
                GLFW.GLFW_KEY_M,
                "category.eventui"
        ));
    }

    private void registerKeyHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Verificar si se presionó la tecla
            while (openMissionsKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new MissionListScreen());
                }
            }
        });
    }
}
