package com.eventui.fabric.client.ui;

import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.eventui.fabric.client.viewmodel.EventViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Pantalla principal de eventos usando la nueva arquitectura.
 */
public class EventScreen extends Screen {

    private final EventViewModel viewModel;
    private List<EventViewModel.EventData> events;

    public EventScreen() {
        super(Component.literal("Events"));

        var player = Minecraft.getInstance().player;
        if (player != null) {
            // Usar ViewModel global del bridge (NO crear uno nuevo)
            this.viewModel = ClientEventBridge.getInstance().getOrCreateViewModel(player.getUUID());
            this.events = viewModel.getAllEvents(); // Obtener eventos actuales

            // Suscribirse a cambios
            viewModel.addChangeListener(this::onEventsUpdated);

            // Si está vacío, solicitar eventos
            if (this.events.isEmpty()) {
                viewModel.requestEvents();
            }
        } else {
            this.viewModel = null;
            this.events = List.of();
        }
    }

    @Override
    protected void init() {
        super.init();

        // Botón de cerrar
        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose()
        ).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    /**
     * Callback cuando se actualizan los eventos.
     */
    private void onEventsUpdated(List<EventViewModel.EventData> newEvents) {
        Minecraft.getInstance().execute(() -> {
            this.events = newEvents;
        });
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // VACÍO - evita que se renderice dos veces el blur
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. BLUR DEL MUNDO (una sola vez al inicio)
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 2. FONDO SEMI-TRANSPARENTE (opcional)
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // 3. TÍTULO
        graphics.drawCenteredString(
                this.font,
                "§6§lEVENTS",
                this.width / 2,
                20,
                0xFFFFFF
        );

        int yOffset = 50;

        // 4. CONTENIDO
        if (events.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    "§7No events available",
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFF
            );
        } else {
            // Renderizar cada evento
            for (EventViewModel.EventData event : events) {
                yOffset = renderEvent(graphics, event, yOffset);
            }
        }

        // 5. BOTONES AL FINAL
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Renderiza un evento individual.
     */
    private int renderEvent(GuiGraphics graphics, EventViewModel.EventData event, int y) {
        int leftMargin = 40;
        int rightMargin = this.width - 40;

        // Fondo del evento (caja con borde)
        graphics.fill(leftMargin - 5, y - 5, rightMargin, y + 60, 0xDD222222);
        graphics.fill(leftMargin - 5, y - 5, rightMargin, y - 4, 0xFFFFAA00); // Borde superior

        // Color según estado
        String titleColor = switch (event.state) {
            case IN_PROGRESS -> "§e";
            case COMPLETED -> "§a";
            case AVAILABLE -> "§f";
            case LOCKED -> "§8";
            default -> "§7";
        };

        // Título del evento
        graphics.drawString(
                this.font,
                titleColor + "● " + event.displayName,
                leftMargin,
                y,
                0xFFFFFF
        );
        y += 12;

        // Descripción
        graphics.drawString(
                this.font,
                "§7" + event.description,
                leftMargin + 10,
                y,
                0xFFFFFF
        );
        y += 12;

        // NUEVO: Descripción del objetivo actual
        if (event.state == com.eventui.api.event.EventState.IN_PROGRESS && event.currentObjectiveDescription != null) {
            graphics.drawString(
                    this.font,
                    "§e→ " + event.currentObjectiveDescription,
                    leftMargin + 10,
                    y,
                    0xFFFFFF
            );
            y += 12;
        }

        // Progreso
        if (event.state == com.eventui.api.event.EventState.IN_PROGRESS) {
            String progressText = String.format("§6Progress: %d/%d (%.0f%%)",
                    event.currentProgress,
                    event.targetProgress,
                    event.getProgressPercentage() * 100
            );

            graphics.drawString(
                    this.font,
                    progressText,
                    leftMargin + 10,
                    y,
                    0xFFFFFF
            );
            y += 12;

            // Barra de progreso visual
            renderProgressBar(graphics, leftMargin + 10, y, 200, 8, event.getProgressPercentage());
            y += 12;
        } else if (event.state == com.eventui.api.event.EventState.COMPLETED) {
            graphics.drawString(
                    this.font,
                    "§a✓ COMPLETED",
                    leftMargin + 10,
                    y,
                    0xFFFFFF
            );
            y += 12;
        }

        y += 20; // Espaciado entre eventos
        return y;
    }

    /**
     * Renderiza una barra de progreso.
     */
    private void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        // Fondo (gris oscuro)
        graphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);

        // Barra de progreso (amarillo/verde)
        int fillWidth = (int) (width * progress);
        int color = progress >= 1.0f ? 0xFF00AA00 : 0xFFFFAA00;
        graphics.fill(x, y, x + fillWidth, y + height, color);

        // Borde brillante
        graphics.fill(x, y, x + width, y + 1, 0xFF666666); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF444444); // Bottom
        graphics.fill(x, y, x + 1, y + height, 0xFF666666); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF666666); // Right
    }

    @Override
    public void onClose() {
        if (viewModel != null) {
            // NO llamar dispose(), solo remover este listener
            viewModel.removeChangeListener(this::onEventsUpdated);
        }
        super.onClose();
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
