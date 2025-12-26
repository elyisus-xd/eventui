package com.eventui.fabric.client.ui;

import com.eventui.api.ui.UIConfig;
import com.eventui.api.ui.UIElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pantalla que renderiza UI desde configuración.
 * FASE 4A: Sistema declarativo de UI.
 */
public class ConfigurableUIScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableUIScreen.class);

    private final UIConfig uiConfig;
    private final UIElementRenderer renderer;

    public ConfigurableUIScreen(UIConfig config) {
        super(Component.literal(config.getTitle()));
        this.uiConfig = config;
        this.renderer = new UIElementRenderer();

        LOGGER.info("Created ConfigurableUIScreen with config: {}", config.getId());
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
        // 1. BLUR DEL MUNDO (una sola vez al inicio)
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 2. Fondo semi-transparente
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // 3. Renderizar todos los elementos
        for (UIElement element : uiConfig.getRootElements()) {
            renderer.render(element, graphics, this.font, mouseX, mouseY);
        }

        // 4. Renderizar botones nativos (si hay)
        super.render(graphics, mouseX, mouseY, partialTick);
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
    public boolean isPauseScreen() {
        String pauseGame = uiConfig.getScreenProperties().get("pause_game");
        return !"false".equalsIgnoreCase(pauseGame);
    }
}
