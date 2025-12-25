package com.eventui.fabric.client;

import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.eventui.fabric.client.bridge.NetworkHandler;
import com.eventui.fabric.client.ui.EventScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUIClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("EventUI-Client");

    private static KeyMapping openEventsKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing EventUI client...");

        try {
            // Registrar payload type
            NetworkHandler.registerPayloadType();

            // Inicializar bridge
            ClientEventBridge.getInstance();

            // Registrar keybinds
            LOGGER.info("Registering keybinds...");
            registerKeybinds();

        } catch (Exception e) {
            LOGGER.error("ERROR during initialization!", e);
        }

        LOGGER.info("EventUI client initialized!");
    }

    private void registerKeybinds() {
        // Crear keybind (tecla 'K' por defecto)
        openEventsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.eventui.open_events",
                GLFW.GLFW_KEY_K,
                "category.eventui"
        ));

        LOGGER.info("Keybind object created: {}", openEventsKey);

        // Registrar handler para cuando se presiona la tecla
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openEventsKey.consumeClick()) {
                LOGGER.info("=== EVENT SCREEN KEYBIND PRESSED! ===");
                if (client.player != null) {
                    client.setScreen(new EventScreen());
                }
            }
        });

        LOGGER.info("Registered keybinds successfully: K = Open Events");
    }
}
