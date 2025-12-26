package com.eventui.fabric.client.ui;

import com.eventui.api.ui.UIElement;
import com.eventui.api.ui.UIElementType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Renderiza elementos UI según su tipo.
 * FASE 4A: Sistema declarativo de renderizado.
 */
public class UIElementRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIElementRenderer.class);

    /**
     * Renderiza un elemento UI y sus hijos.
     */
    public void render(UIElement element, GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!element.isVisible()) {
            return;
        }

        switch (element.getType()) {
            case IMAGE -> renderImage(element, graphics);
            case TEXT -> renderText(element, graphics, font);
            case BUTTON -> renderButton(element, graphics, font, mouseX, mouseY);
            case PROGRESS_BAR -> renderProgressBar(element, graphics);
            case PANEL -> renderPanel(element, graphics, font, mouseX, mouseY);
            default -> LOGGER.warn("Unsupported element type: {}", element.getType());
        }

        // Renderizar hijos (recursivo)
        for (UIElement child : element.getChildren()) {
            render(child, graphics, font, mouseX, mouseY);
        }
    }

    /**
     * Renderiza una imagen/textura.
     */
    private void renderImage(UIElement element, GuiGraphics graphics) {
        String texture = element.getProperties().get("texture");

        if (texture == null) {
            LOGGER.warn("IMAGE element {} missing 'texture' property", element.getId());
            return;
        }

        // Por ahora, renderizar un rectángulo de color (placeholder)
        // TODO: Cargar y renderizar textura real
        int color = parseColor(element.getProperties().getOrDefault("color", "808080"));
        graphics.fill(
                element.getX(),
                element.getY(),
                element.getX() + element.getWidth(),
                element.getY() + element.getHeight(),
                color
        );
    }

    /**
     * Renderiza texto.
     */
    private void renderText(UIElement element, GuiGraphics graphics, Font font) {
        String content = element.getProperties().get("content");

        if (content == null) {
            content = "Missing content";
        }

        // Procesar códigos de color Minecraft
        String align = element.getProperties().getOrDefault("align", "left");
        boolean shadow = Boolean.parseBoolean(element.getProperties().getOrDefault("shadow", "true"));

        int x = element.getX();
        int y = element.getY();

        // Alineación
        if ("center".equalsIgnoreCase(align)) {
            int textWidth = font.width(content);
            x = element.getX() + (element.getWidth() / 2) - (textWidth / 2);
        } else if ("right".equalsIgnoreCase(align)) {
            int textWidth = font.width(content);
            x = element.getX() + element.getWidth() - textWidth;
        }

        if (shadow) {
            graphics.drawString(font, content, x, y, 0xFFFFFF);
        } else {
            graphics.drawString(font, content, x, y, 0xFFFFFF, false);
        }
    }

    /**
     * Renderiza un botón.
     */
    private void renderButton(UIElement element, GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        boolean isHovered = isMouseOver(element, mouseX, mouseY);

        // Color del botón (cambia en hover)
        int backgroundColor = isHovered ? 0xFFFFAA00 : 0xFF555555;
        int borderColor = isHovered ? 0xFFFFFF00 : 0xFF888888;

        // Fondo del botón
        graphics.fill(
                element.getX(),
                element.getY(),
                element.getX() + element.getWidth(),
                element.getY() + element.getHeight(),
                backgroundColor
        );

        // Borde
        graphics.fill(element.getX(), element.getY(),
                element.getX() + element.getWidth(), element.getY() + 1, borderColor);
        graphics.fill(element.getX(), element.getY() + element.getHeight() - 1,
                element.getX() + element.getWidth(), element.getY() + element.getHeight(), borderColor);
        graphics.fill(element.getX(), element.getY(),
                element.getX() + 1, element.getY() + element.getHeight(), borderColor);
        graphics.fill(element.getX() + element.getWidth() - 1, element.getY(),
                element.getX() + element.getWidth(), element.getY() + element.getHeight(), borderColor);

        // Texto del botón
        String text = element.getProperties().getOrDefault("text", "Button");
        int textWidth = font.width(text);
        int textX = element.getX() + (element.getWidth() / 2) - (textWidth / 2);
        int textY = element.getY() + (element.getHeight() / 2) - 4;

        graphics.drawString(font, text, textX, textY, 0xFFFFFF);
    }

    /**
     * Renderiza una barra de progreso.
     */
    private void renderProgressBar(UIElement element, GuiGraphics graphics) {
        // Obtener progreso (0.0 - 1.0)
        float progress = Float.parseFloat(element.getProperties().getOrDefault("progress", "0.0"));
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        // Fondo
        graphics.fill(
                element.getX(),
                element.getY(),
                element.getX() + element.getWidth(),
                element.getY() + element.getHeight(),
                0xFF1A1A1A
        );

        // Barra de progreso
        int fillWidth = (int) (element.getWidth() * progress);
        int color = progress >= 1.0f ? 0xFF00AA00 : 0xFFFFAA00;
        graphics.fill(
                element.getX(),
                element.getY(),
                element.getX() + fillWidth,
                element.getY() + element.getHeight(),
                color
        );

        // Bordes
        graphics.fill(element.getX(), element.getY(),
                element.getX() + element.getWidth(), element.getY() + 1, 0xFF666666);
        graphics.fill(element.getX(), element.getY() + element.getHeight() - 1,
                element.getX() + element.getWidth(), element.getY() + element.getHeight(), 0xFF444444);
    }

    /**
     * Renderiza un panel (contenedor).
     */
    private void renderPanel(UIElement element, GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        // Fondo del panel (opcional)
        String bgColor = element.getProperties().get("background_color");
        if (bgColor != null) {
            int color = parseColor(bgColor);
            graphics.fill(
                    element.getX(),
                    element.getY(),
                    element.getX() + element.getWidth(),
                    element.getY() + element.getHeight(),
                    color
            );
        }

        // Los hijos se renderizan automáticamente en el método principal
    }

    /**
     * Verifica si el mouse está sobre un elemento.
     */
    private boolean isMouseOver(UIElement element, int mouseX, int mouseY) {
        return mouseX >= element.getX() && mouseX <= element.getX() + element.getWidth() &&
                mouseY >= element.getY() && mouseY <= element.getY() + element.getHeight();
    }

    /**
     * Parsea un color hexadecimal.
     */
    private int parseColor(String hex) {
        try {
            // Soporta formatos: "RRGGBB" o "AARRGGBB"
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }

            if (hex.length() == 6) {
                hex = "FF" + hex; // Agregar alpha si no existe
            }

            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid color format: {}, using gray", hex);
            return 0xFF808080;
        }
    }
}
