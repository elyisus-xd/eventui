package com.eventui.fabric.client.keybinds;

import com.eventui.fabric.client.ui.EventScreen;
import com.eventui.fabric.client.ui.QuestTrackerHUD;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestiona todos los keybinds de EventUI.
 */
public class EventUIKeybinds {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventUIKeybinds.class);

    private static KeyMapping openEventsKey;
    private static KeyMapping toggleTrackerKey;

    /**
     * Registra todos los keybinds del mod.
     */
    public static void register() {
        LOGGER.info("Registering EventUI keybinds...");

        // Tecla K: Abrir pantalla de eventos
        openEventsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.eventui.openevents",
                GLFW.GLFW_KEY_K,
                "category.eventui"
        ));

        // Tecla H: Toggle Quest Tracker HUD
        toggleTrackerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.eventui.toggle_tracker",
                GLFW.GLFW_KEY_H,
                "category.eventui"
        ));

        LOGGER.info("Keybind objects created: openEventsKey={}, toggleTrackerKey={}",
                openEventsKey, toggleTrackerKey);

        // Registrar event listener para detectar presión de teclas
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Abrir pantalla de eventos (K)
            while (openEventsKey.consumeClick()) {
                handleOpenEvents(client);
            }

            // Toggle Quest Tracker (H)
            while (toggleTrackerKey.consumeClick()) {
                handleToggleTracker(client);
            }
        });

        LOGGER.info("EventUI Keybinds registered successfully!");
        LOGGER.info("  - K: Open Events Screen");
        LOGGER.info("  - H: Toggle Quest Tracker");
    }

    /**
     * Maneja la tecla K: Abrir pantalla de eventos.
     */
    private static void handleOpenEvents(Minecraft client) {
        if (client.player != null) {
            LOGGER.info("Opening Events Screen...");
            client.setScreen(new EventScreen());

            // Forzar actualización del HUD también
            QuestTrackerHUD.forceUpdate();
        }
    }

    /**
     * Maneja la tecla H: Toggle Quest Tracker.
     */
    private static void handleToggleTracker(Minecraft client) {
        if (client.player != null) {
            QuestTrackerHUD.toggle();
        }
    }
}
