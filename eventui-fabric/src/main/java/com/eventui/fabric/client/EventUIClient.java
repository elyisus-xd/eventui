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
     * FASE 4B: Test de UI configurable con data binding.
     */
    private void testConfigurableUI(net.minecraft.client.Minecraft client) {
        // Crear UI de prueba con DATA BINDING
        java.util.List<com.eventui.api.ui.UIElement> elements = new java.util.ArrayList<>();

        // Título con binding
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "title",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 20, 200, 20,
                java.util.Map.of("content", "§6§l{{event.displayName}}", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Descripción del evento
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "description",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 40, 200, 20,
                java.util.Map.of("content", "§7{{event.description}}", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Contador de eventos
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "event_count",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 60, 200, 20,
                java.util.Map.of("content", "§7Total: {{event_count}} events", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Barra de progreso con binding
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "event_progress",
                com.eventui.api.ui.UIElementType.PROGRESS_BAR,
                60, 100, 200, 10,
                java.util.Map.of("progress", "{{progress.percentage}}"),
                java.util.List.of(),
                true,
                5
        ));

        // Texto de progreso con binding
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "progress_text",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 120, 200, 20,
                java.util.Map.of("content", "§6Progress: {{progress.current}}/{{progress.target}} ({{progress.percentage}}%)", "align", "center", "shadow", "true"),
                java.util.List.of(),
                true,
                10
        ));

        // Estado del evento
        elements.add(new com.eventui.core.v2.config.UIElementImpl(
                "event_state",
                com.eventui.api.ui.UIElementType.TEXT,
                160, 140, 200, 20,
                java.util.Map.of("content", "§eState: §f{{event.state}}", "align", "center", "shadow", "true"),
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
                "test_ui_binding",
                "Test UI with Data Binding",
                320, 240,
                elements,
                null, // Sin evento asociado, usará el primero disponible
                java.util.Map.of("blur_background", "true")
        );

        client.setScreen(new com.eventui.fabric.client.ui.ConfigurableUIScreen(testConfig));

        LOGGER.info("Opened configurable UI with data binding ({} elements)", elements.size());
    }


    /**
     * FASE 4A: Abre una UI configurable (para testing).
     */
    public static void openConfigurableUI(com.eventui.api.ui.UIConfig config) {
        var client = net.minecraft.client.Minecraft.getInstance();
        client.setScreen(new com.eventui.fabric.client.ui.ConfigurableUIScreen(config));
    }
}
