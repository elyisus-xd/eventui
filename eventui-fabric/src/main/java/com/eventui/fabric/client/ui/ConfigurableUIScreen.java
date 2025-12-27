package com.eventui.fabric.client.ui;

import com.eventui.api.ui.UIConfig;
import com.eventui.api.ui.UIElement;
import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.eventui.fabric.client.viewmodel.EventViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla que renderiza UI desde configuración.
 * FASE 4B: Con data binding dinámico.
 */
public class ConfigurableUIScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableUIScreen.class);

    private final UIConfig uiConfig;
    private final UIElementRenderer renderer;
    private final EventViewModel viewModel;
    private List<EventViewModel.EventData> events;

    public ConfigurableUIScreen(UIConfig config) {
        super(Component.literal(config.getTitle()));
        this.uiConfig = config;
        this.renderer = new UIElementRenderer();

        // ✅ NUEVO: Conectar con EventViewModel
        var player = Minecraft.getInstance().player;
        if (player != null) {
            this.viewModel = ClientEventBridge.getInstance().getOrCreateViewModel(player.getUUID());
            this.events = viewModel.getAllEvents();

            // Suscribirse a cambios
            viewModel.addChangeListener(this::onEventsUpdated);

            // Solicitar eventos si está vacío
            if (this.events.isEmpty()) {
                viewModel.requestEvents();
            }
        } else {
            this.viewModel = null;
            this.events = List.of();
        }

        LOGGER.info("Created ConfigurableUIScreen with config: {}, events: {}", config.getId(), events.size());
    }

    /**
     * ✅ NUEVO: Callback cuando se actualizan eventos.
     */
    private void onEventsUpdated(List<EventViewModel.EventData> newEvents) {
        Minecraft.getInstance().execute(() -> {
            this.events = newEvents;
            LOGGER.info("UI updated with {} events", events.size());
        });
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("Initialized UI: {}, elements: {}", uiConfig.getId(), uiConfig.getRootElements().size());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // VACÍO - evita doble blur
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. BLUR DEL MUNDO (una sola vez)
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 2. Fondo semi-transparente
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // ✅ NUEVO: Crear contexto de datos
        Map<String, Object> context = createDataContext();

        // 3. Renderizar todos los elementos CON CONTEXTO
        for (UIElement element : uiConfig.getRootElements()) {
            renderer.render(element, graphics, this.font, mouseX, mouseY, context);
        }

        // 4. Renderizar botones nativos
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * ✅ NUEVO: Crea el contexto de datos para data binding.
     */
    private Map<String, Object> createDataContext() {
        Map<String, Object> context = new HashMap<>();

        // Datos globales
        context.put("event_count", events.size());
        context.put("events", events);

        // Si hay un evento asociado a esta UI, agregarlo
        if (uiConfig.getAssociatedEventId() != null && !events.isEmpty()) {
            var associatedEvent = events.stream()
                    .filter(e -> e.id.equals(uiConfig.getAssociatedEventId()))
                    .findFirst()
                    .orElse(null);

            if (associatedEvent != null) {
                context.put("event", associatedEvent);
                context.put("progress", Map.of(
                        "current", associatedEvent.currentProgress,
                        "target", associatedEvent.targetProgress,
                        "percentage", associatedEvent.getProgressPercentage() * 100
                ));
            }
        } else if (!events.isEmpty()) {
            // Si no hay evento asociado, usar el primero como ejemplo
            var firstEvent = events.get(0);
            context.put("event", firstEvent);
            context.put("progress", Map.of(
                    "current", firstEvent.currentProgress,
                    "target", firstEvent.targetProgress,
                    "percentage", firstEvent.getProgressPercentage() * 100
            ));
        }

        return context;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Detectar clicks en botones
        for (UIElement element : uiConfig.getRootElements()) {
            if (element.getType() == com.eventui.api.ui.UIElementType.BUTTON) {
                if (isMouseOver(element, (int) mouseX, (int) mouseY)) {
                    handleButtonClick(element);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Maneja el click en un botón.
     */
    private void handleButtonClick(UIElement button) {
        String action = button.getProperties().get("action");

        LOGGER.info("Button clicked: {}, action: {}", button.getId(), action);

        if ("close_screen".equals(action)) {
            this.onClose();
        }
        // TODO: Más acciones (open_submenu, etc.)
    }

    /**
     * Verifica si el mouse está sobre un elemento.
     */
    private boolean isMouseOver(UIElement element, int mouseX, int mouseY) {
        return mouseX >= element.getX() && mouseX <= element.getX() + element.getWidth() &&
                mouseY >= element.getY() && mouseY <= element.getY() + element.getHeight();
    }

    @Override
    public void onClose() {
        // ✅ NUEVO: Desuscribirse al cerrar
        if (viewModel != null) {
            viewModel.removeChangeListener(this::onEventsUpdated);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        String pauseGame = uiConfig.getScreenProperties().get("pause_game");
        return !"false".equalsIgnoreCase(pauseGame);
    }
}
