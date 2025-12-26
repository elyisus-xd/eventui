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
        // Crear keybind (tecla K por defecto)
        openEventsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.eventui.openevents",
                GLFW.GLFW_KEY_K,
                "category.eventui"
        ));

        LOGGER.info("Keybind object created: {}", openEventsKey);

        // Registrar handler para cuando se presiona la tecla
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openEventsKey.consumeClick()) {
                LOGGER.info("=== EVENT SCREEN KEYBIND PRESSED! ===");

                if (client.player != null) {
                    // ✅ FASE 4A: Test con UI configurable
                    testConfigurableUI(client);
                }
            }
        });

        LOGGER.info("Registered keybinds successfully (K = Open Events)");
    }

    /**
     * FASE 4A: Test de UI configurable.
     */
    private void testConfigurableUI(net.minecraft.client.Minecraft client) {
        // Crear UI de prueba manualmente
        java.util.List<com.eventui.api.ui.UIElement> elements = new java.util.ArrayList<>();

        // Título
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "title",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 20, 200, 20,
                java.util.Map.of("content", "§6§lTEST UI - FASE 4A", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Subtítulo
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "subtitle",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 40, 200, 20,
                java.util.Map.of("content", "§7UI Configurable Funcionando", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Barra de progreso de ejemplo
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "test_progress",
                com.eventui.api.ui.UIElementType.PROGRESS_BAR,
                60, 100, 200, 10,
                java.util.Map.of("progress", "0.75"),
                java.util.List.of(),
                true,
                5
        ));

        // Texto de progreso
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "progress_text",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 120, 200, 20,
                java.util.Map.of("content", "§6Progress: 75%", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Botón Close
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "close_btn",
                com.eventui.api.ui.UIElementType.BUTTON,
                135, 200, 50, 20,
                java.util.Map.of("text", "Close", "action", "close_screen"),
                java.util.List.of(),
                true,
                100
        ));

        com.eventui.api.ui.UIConfig testConfig = new com.eventui.core.v2.config.UIConfigImpl(
                "test_ui",
                "Test UI",
                320, 240,
                elements,
                null,
                java.util.Map.of("blur_background", "true")
        );

        client.setScreen(new com.eventui.fabric.client.ui.ConfigurableUIScreen(testConfig));

        LOGGER.info("Opened configurable UI with {} elements", elements.size());
    }

    /**
     * FASE 4A: Abre una UI configurable (para testing).
     */
    public static void openConfigurableUI(com.eventui.api.ui.UIConfig config) {
        var client = net.minecraft.client.Minecraft.getInstance();
        client.setScreen(new com.eventui.fabric.client.ui.ConfigurableUIScreen(config));
    }
}
