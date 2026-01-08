package com.eventui.fabric.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Pantalla de confirmación para abandonar eventos no-repeatables.
 */
public class ConfirmAbandonScreen extends Screen {

    private final Screen parentScreen;
    private final String eventName;
    private final Runnable onConfirm;

    // Botones personalizados
    private CustomButton confirmButton;
    private CustomButton cancelButton;

    public ConfirmAbandonScreen(Screen parentScreen, String eventName, Runnable onConfirm) {
        super(Component.literal("Confirm Abandon"));
        this.parentScreen = parentScreen;
        this.eventName = eventName;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Crear botones personalizados
        confirmButton = new CustomButton(
                centerX - 155,
                centerY + 40,
                150,
                22,
                "Confirm Abandon",
                0xFFDD0000, // Rojo
                () -> {
                    onConfirm.run();
                    this.onClose();
                }
        );

        cancelButton = new CustomButton(
                centerX + 5,
                centerY + 40,
                150,
                22,
                "Cancel",
                0xFF555555, // Gris
                this::onClose
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            if (confirmButton.isMouseOver(mouseX, mouseY)) {
                confirmButton.onClick();
                return true;
            }
            if (cancelButton.isMouseOver(mouseX, mouseY)) {
                cancelButton.onClick();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // VACÍO - No llamar super para evitar blur
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fondo oscuro manual (sin blur)
        graphics.fill(0, 0, this.width, this.height, 0xD0000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Cuadro de diálogo
        int boxWidth = 340;
        int boxHeight = 140;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // Fondo del cuadro
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xEE1A1A1A);

        // Borde superior rojo (más grueso)
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 3, 0xFFDD0000);

        // Bordes laterales rojos
        graphics.fill(boxX, boxY, boxX + 2, boxY + boxHeight, 0xFFDD0000);
        graphics.fill(boxX + boxWidth - 2, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFDD0000);

        // Borde inferior
        graphics.fill(boxX, boxY + boxHeight - 2, boxX + boxWidth, boxY + boxHeight, 0xFF880000);

        // Título con fondo
        int titleBarHeight = 25;
        graphics.fill(boxX + 2, boxY + 3, boxX + boxWidth - 2, boxY + titleBarHeight, 0xAA330000);

        graphics.drawCenteredString(
                this.font,
                "§c§l⚠ WARNING",
                centerX,
                boxY + 10,
                0xFFFFFF
        );

        // Mensaje de advertencia
        String[] lines = {
                "§7You are about to abandon:",
                "§e" + eventName,
                "",
                "§c§lThis event is NOT repeatable.",
                "§7It will be marked as §cFAILED §7and",
                "§7cannot be restarted."
        };

        int lineY = boxY + 35;
        for (String line : lines) {
            graphics.drawCenteredString(this.font, line, centerX, lineY, 0xFFFFFF);
            lineY += 11;
        }

        // Renderizar botones personalizados
        confirmButton.render(graphics, mouseX, mouseY, this.font);
        cancelButton.render(graphics, mouseX, mouseY, this.font);

        // NO llamar super.render() para evitar widgets de Minecraft
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Botón personalizado estilizado.
     */
    private static class CustomButton {
        private final int x, y, width, height;
        private final String text;
        private final int baseColor;
        private final Runnable onClick;

        public CustomButton(int x, int y, int width, int height, String text, int baseColor, Runnable onClick) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.baseColor = baseColor;
            this.onClick = onClick;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                    mouseY >= y && mouseY <= y + height;
        }

        public void onClick() {
            this.onClick.run();
        }

        public void render(GuiGraphics graphics, int mouseX, int mouseY, net.minecraft.client.gui.Font font) {
            boolean hovered = isMouseOver(mouseX, mouseY);

            // Color según hover
            int bgColor = hovered ? (baseColor | 0xFF000000) : (baseColor & 0xDDFFFFFF);

            // Fondo del botón
            graphics.fill(x, y, x + width, y + height, bgColor);

            // Bordes estilizados
            if (hovered) {
                // Borde brillante cuando está hover
                graphics.fill(x, y, x + width, y + 2, 0xFFFFFFFF);
                graphics.fill(x, y + height - 2, x + width, y + height, 0xFF666666);
                graphics.fill(x, y, x + 2, y + height, 0xFFDDDDDD);
                graphics.fill(x + width - 2, y, x + width, y + height, 0xFF999999);
            } else {
                // Borde normal
                graphics.fill(x, y, x + width, y + 1, 0xFF888888);
                graphics.fill(x, y + height - 1, x + width, y + height, 0xFF333333);
            }

            // Texto centrado
            int textColor = hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
            int textX = x + (width / 2) - (font.width(text) / 2);
            int textY = y + (height / 2) - 4;

            // Sombra del texto
            graphics.drawString(font, text, textX + 1, textY + 1, 0x88000000);
            graphics.drawString(font, text, textX, textY, textColor);
        }
    }
}
