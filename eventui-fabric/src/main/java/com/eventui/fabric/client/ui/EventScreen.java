package com.eventui.fabric.client.ui;

import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.eventui.fabric.client.viewmodel.EventViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal de eventos usando la nueva arquitectura.
 * Muestra múltiples eventos apilados verticalmente.
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

            // DEBUG: Imprimir cantidad de eventos
            System.out.println("[EventScreen] Constructor - Eventos cargados: " + events.size());
            for (int i = 0; i < events.size(); i++) {
                EventViewModel.EventData event = events.get(i);
                System.out.println("  [" + i + "] " + event.displayName + " - Estado: " + event.state);
            }

            // Suscribirse a cambios
            viewModel.addChangeListener(this::onEventsUpdated);

            // Si está vacío, solicitar eventos
            if (this.events.isEmpty()) {
                System.out.println("[EventScreen] Lista vacía, solicitando eventos...");
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
            System.out.println("[EventScreen] onEventsUpdated - Nuevos eventos: " + newEvents.size());
            this.events = newEvents;
            for (int i = 0; i < events.size(); i++) {
                EventViewModel.EventData event = events.get(i);
                System.out.println("  [" + i + "] " + event.displayName + " - Estado: " + event.state);
            }
        });
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // VACÍO - evita que se renderice dos veces el blur
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1. BLUR DEL MUNDO
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 2. FONDO SEMI-TRANSPARENTE
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // 3. TÍTULO
        graphics.drawCenteredString(
                this.font,
                "§6§lEVENTS",
                this.width / 2,
                20,
                0xFFFFFF
        );

        // DEBUG: Mostrar cantidad de eventos
        String debugInfo = String.format("§7Total events: %d", events.size());
        graphics.drawCenteredString(
                this.font,
                debugInfo,
                this.width / 2,
                35,
                0xFFFFFF
        );

        int yOffset = 55;

        // 4. ORGANIZAR EVENTOS POR ESTADO
        if (events.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    "§7No events available",
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFF
            );
        } else {
            // Separar eventos por estado
            List<EventViewModel.EventData> inProgress = new ArrayList<>();
            List<EventViewModel.EventData> available = new ArrayList<>();
            List<EventViewModel.EventData> completed = new ArrayList<>();
            List<EventViewModel.EventData> locked = new ArrayList<>();

            for (EventViewModel.EventData event : events) {
                switch (event.state) {
                    case IN_PROGRESS -> inProgress.add(event);
                    case AVAILABLE -> available.add(event);
                    case COMPLETED -> completed.add(event);
                    case LOCKED -> locked.add(event);
                }
            }

            // SECCIÓN: EN PROGRESO
            if (!inProgress.isEmpty()) {
                graphics.drawString(
                        this.font,
                        "§e§lIN PROGRESS",
                        45,
                        yOffset,
                        0xFFFFFF
                );
                yOffset += 15;

                for (EventViewModel.EventData event : inProgress) {
                    yOffset = renderEvent(graphics, event, yOffset);
                    yOffset += 10;
                }
                yOffset += 10; // Espacio extra entre secciones
            }

            // SECCIÓN: DISPONIBLES
            if (!available.isEmpty()) {
                graphics.drawString(
                        this.font,
                        "§f§lAVAILABLE",
                        45,
                        yOffset,
                        0xFFFFFF
                );
                yOffset += 15;

                for (EventViewModel.EventData event : available) {
                    yOffset = renderEvent(graphics, event, yOffset);
                    yOffset += 10;
                }
                yOffset += 10;
            }

            // SECCIÓN: COMPLETADOS
            if (!completed.isEmpty()) {
                graphics.drawString(
                        this.font,
                        "§a§lCOMPLETED",
                        45,
                        yOffset,
                        0xFFFFFF
                );
                yOffset += 15;

                for (EventViewModel.EventData event : completed) {
                    yOffset = renderEvent(graphics, event, yOffset);
                    yOffset += 10;
                }
                yOffset += 10;
            }

            // SECCIÓN: BLOQUEADOS
            if (!locked.isEmpty()) {
                graphics.drawString(
                        this.font,
                        "§8§lLOCKED",
                        45,
                        yOffset,
                        0xFFFFFF
                );
                yOffset += 15;

                for (EventViewModel.EventData event : locked) {
                    yOffset = renderEvent(graphics, event, yOffset);
                    yOffset += 10;
                }
            }
        }

        // 5. BOTONES
        super.render(graphics, mouseX, mouseY, partialTick);
    }


    /**
     * Renderiza un evento individual.
     */
    private int renderEvent(GuiGraphics graphics, EventViewModel.EventData event, int y) {
        int leftMargin = 40;
        int rightMargin = this.width - 40;

        // Calcular altura dinámica del evento
        int eventHeight = 60; // Altura base
        if (event.state == com.eventui.api.event.EventState.IN_PROGRESS) {
            eventHeight += 24; // Espacio extra para progreso
            if (event.currentObjectiveDescription != null) {
                eventHeight += 12; // Espacio extra para descripción de objetivo
            }
        }

        // Fondo del evento (caja con borde)
        graphics.fill(leftMargin - 5, y - 5, rightMargin, y + eventHeight, 0xDD222222);
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

// ✅ ACTUALIZADO: Mostrar descripción del objetivo en AVAILABLE e IN_PROGRESS
        if (event.currentObjectiveDescription != null &&
                (event.state == com.eventui.api.event.EventState.AVAILABLE ||
                        event.state == com.eventui.api.event.EventState.IN_PROGRESS)) {
            graphics.drawString(
                    this.font,
                    "§e→ " + event.currentObjectiveDescription,
                    leftMargin + 10,
                    y,
                    0xFFFFFF
            );
            y += 12;
        }

// Progreso (solo si está IN_PROGRESS)
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

        y += 15; // Espaciado entre eventos
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
