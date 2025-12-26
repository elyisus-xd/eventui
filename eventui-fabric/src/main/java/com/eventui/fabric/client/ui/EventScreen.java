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
 * FASE 3: Con scroll para múltiples eventos.
 */
public class EventScreen extends Screen {

    private final EventViewModel viewModel;
    private List<EventViewModel.EventData> events;

    // FASE 3: Variables de scroll
    private double scrollOffset = 0;
    private double maxScrollOffset = 0;
    private static final int SCROLL_SPEED = 20;
    private static final int TOP_MARGIN = 55;
    private static final int BOTTOM_MARGIN = 50;

    public EventScreen() {
        super(Component.literal("Events"));

        var player = Minecraft.getInstance().player;
        if (player != null) {
            this.viewModel = ClientEventBridge.getInstance().getOrCreateViewModel(player.getUUID());
            this.events = viewModel.getAllEvents();

            System.out.println("[EventScreen] Constructor - Eventos cargados: " + events.size());
            for (int i = 0; i < events.size(); i++) {
                EventViewModel.EventData event = events.get(i);
                System.out.println("  [" + i + "] " + event.displayName + " - Estado: " + event.state);
            }

            viewModel.addChangeListener(this::onEventsUpdated);

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

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose()
        ).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());

        updateMaxScroll();
    }

    private void onEventsUpdated(List<EventViewModel.EventData> newEvents) {
        Minecraft.getInstance().execute(() -> {
            System.out.println("[EventScreen] onEventsUpdated - Nuevos eventos: " + newEvents.size());
            this.events = newEvents;
            for (int i = 0; i < events.size(); i++) {
                EventViewModel.EventData event = events.get(i);
                System.out.println("  [" + i + "] " + event.displayName + " - Estado: " + event.state);
            }
            updateMaxScroll();
        });
    }

    /**
     * FASE 3: Calcula el scroll máximo basado en el contenido.
     */
    private void updateMaxScroll() {
        if (this.height == 0) return;

        int contentHeight = calculateContentHeight();
        int visibleHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
        maxScrollOffset = Math.max(0, contentHeight - visibleHeight);

        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }

    /**
     * FASE 3: Calcula la altura total del contenido.
     */
    private int calculateContentHeight() {
        int totalHeight = 0;

        List<EventViewModel.EventData> available = new ArrayList<>();
        List<EventViewModel.EventData> inProgress = new ArrayList<>();
        List<EventViewModel.EventData> completed = new ArrayList<>();

        for (EventViewModel.EventData event : events) {
            switch (event.state) {
                case AVAILABLE -> available.add(event);
                case IN_PROGRESS -> inProgress.add(event);
                case COMPLETED -> completed.add(event);
            }
        }

        if (!inProgress.isEmpty()) {
            totalHeight += 15;
            totalHeight += inProgress.size() * 110;
        }

        if (!available.isEmpty()) {
            totalHeight += 15;
            totalHeight += available.size() * 80;
        }

        if (!completed.isEmpty()) {
            totalHeight += 15;
            totalHeight += completed.size() * 70;
        }

        return totalHeight;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScrollOffset > 0) {
            scrollOffset -= verticalAmount * SCROLL_SPEED;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // VACÍO
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        graphics.drawCenteredString(
                this.font,
                "§6§lEVENTS",
                this.width / 2,
                20,
                0xFFFFFF
        );

        String debugInfo = String.format("§7Total events: %d", events.size());
        graphics.drawCenteredString(
                this.font,
                debugInfo,
                this.width / 2,
                35,
                0xFFFFFF
        );

        if (events.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    "§7No events available",
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFF
            );
        } else {
            renderScrollableContent(graphics);
        }

        if (maxScrollOffset > 0) {
            renderScrollIndicator(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * FASE 3: Renderiza el contenido con scroll y clipping.
     */
    private void renderScrollableContent(GuiGraphics graphics) {
        int startY = TOP_MARGIN;
        int endY = this.height - BOTTOM_MARGIN;

        graphics.enableScissor(0, startY, this.width, endY);

        int yOffset = startY - (int)scrollOffset;

        List<EventViewModel.EventData> available = new ArrayList<>();
        List<EventViewModel.EventData> inProgress = new ArrayList<>();
        List<EventViewModel.EventData> completed = new ArrayList<>();

        for (EventViewModel.EventData event : events) {
            switch (event.state) {
                case AVAILABLE -> available.add(event);
                case IN_PROGRESS -> inProgress.add(event);
                case COMPLETED -> completed.add(event);
            }
        }

        if (!inProgress.isEmpty()) {
            if (yOffset > startY - 15 && yOffset < endY) {
                graphics.drawString(this.font, "§e§lIN PROGRESS", 45, yOffset, 0xFFFFFF);
            }
            yOffset += 15;

            for (EventViewModel.EventData event : inProgress) {
                if (yOffset + 110 > startY && yOffset < endY) {
                    yOffset = renderEvent(graphics, event, yOffset);
                } else {
                    yOffset += 110;
                }
                yOffset += 10;
            }
            yOffset += 10;
        }

        if (!available.isEmpty()) {
            if (yOffset > startY - 15 && yOffset < endY) {
                graphics.drawString(this.font, "§f§lAVAILABLE", 45, yOffset, 0xFFFFFF);
            }
            yOffset += 15;

            for (EventViewModel.EventData event : available) {
                if (yOffset + 80 > startY && yOffset < endY) {
                    yOffset = renderEvent(graphics, event, yOffset);
                } else {
                    yOffset += 80;
                }
                yOffset += 10;
            }
            yOffset += 10;
        }

        if (!completed.isEmpty()) {
            if (yOffset > startY - 15 && yOffset < endY) {
                graphics.drawString(this.font, "§a§lCOMPLETED", 45, yOffset, 0xFFFFFF);
            }
            yOffset += 15;

            for (EventViewModel.EventData event : completed) {
                if (yOffset + 70 > startY && yOffset < endY) {
                    yOffset = renderEvent(graphics, event, yOffset);
                } else {
                    yOffset += 70;
                }
                yOffset += 10;
            }
        }

        graphics.disableScissor();
    }

    /**
     * FASE 3: Indicador visual de scroll en el lado derecho.
     */
    private void renderScrollIndicator(GuiGraphics graphics) {
        int barX = this.width - 10;
        int barY = TOP_MARGIN;
        int barHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
        int barWidth = 4;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x40FFFFFF);

        double scrollPercentage = scrollOffset / maxScrollOffset;
        int indicatorHeight = Math.max(20, (int)(barHeight * ((double)barHeight / (barHeight + maxScrollOffset))));
        int indicatorY = barY + (int)((barHeight - indicatorHeight) * scrollPercentage);

        graphics.fill(barX, indicatorY, barX + barWidth, indicatorY + indicatorHeight, 0xFFFFAA00);
    }

    private int renderEvent(GuiGraphics graphics, EventViewModel.EventData event, int y) {
        int leftMargin = 40;
        int rightMargin = this.width - 20;

        int eventHeight = 60;
        if (event.state == com.eventui.api.event.EventState.IN_PROGRESS) {
            eventHeight += 24;
            if (event.currentObjectiveDescription != null) {
                eventHeight += 12;
            }
        }

        graphics.fill(leftMargin - 5, y - 5, rightMargin, y + eventHeight, 0xDD222222);
        graphics.fill(leftMargin - 5, y - 5, rightMargin, y - 4, 0xFFFFAA00);

        String titleColor = switch (event.state) {
            case IN_PROGRESS -> "§e";
            case COMPLETED -> "§a";
            case AVAILABLE -> "§f";
            case LOCKED -> "§8";
            default -> "§7";
        };

        graphics.drawString(this.font, titleColor + "● " + event.displayName, leftMargin, y, 0xFFFFFF);
        y += 12;

        graphics.drawString(this.font, "§7" + event.description, leftMargin + 10, y, 0xFFFFFF);
        y += 12;

        if (event.currentObjectiveDescription != null &&
                (event.state == com.eventui.api.event.EventState.AVAILABLE ||
                        event.state == com.eventui.api.event.EventState.IN_PROGRESS)) {
            graphics.drawString(this.font, "§e→ " + event.currentObjectiveDescription, leftMargin + 10, y, 0xFFFFFF);
            y += 12;
        }

        if (event.state == com.eventui.api.event.EventState.IN_PROGRESS) {
            String progressText = String.format("§6Progress: %d/%d (%.0f%%)",
                    event.currentProgress, event.targetProgress, event.getProgressPercentage() * 100);

            graphics.drawString(this.font, progressText, leftMargin + 10, y, 0xFFFFFF);
            y += 12;

            renderProgressBar(graphics, leftMargin + 10, y, 200, 8, event.getProgressPercentage());
            y += 12;
        } else if (event.state == com.eventui.api.event.EventState.COMPLETED) {
            graphics.drawString(this.font, "§a✓ COMPLETED", leftMargin + 10, y, 0xFFFFFF);
            y += 12;
        }

        y += 15;
        return y;
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        graphics.fill(x, y, x + width, y + height, 0xFF1A1A1A);

        int fillWidth = (int) (width * progress);
        int color = progress >= 1.0f ? 0xFF00AA00 : 0xFFFFAA00;
        graphics.fill(x, y, x + fillWidth, y + height, color);

        graphics.fill(x, y, x + width, y + 1, 0xFF666666);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF444444);
        graphics.fill(x, y, x + 1, y + height, 0xFF666666);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF666666);
    }

    @Override
    public void onClose() {
        if (viewModel != null) {
            viewModel.removeChangeListener(this::onEventsUpdated);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
