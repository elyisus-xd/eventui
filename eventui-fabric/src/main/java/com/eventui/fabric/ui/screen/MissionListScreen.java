package com.eventui.fabric.ui.screen;

import com.eventui.common.dto.DataSnapshot;
import com.eventui.fabric.EventUIFabricMod;
import com.eventui.fabric.viewmodel.UIContextImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MissionListScreen extends Screen {
    private final UIContextImpl uiContext;
    private List<Map<String, Object>> availableMissions = new ArrayList<>();
    private List<Map<String, Object>> activeMissions = new ArrayList<>();
    private List<Map<String, Object>> completedMissions = new ArrayList<>();
    private List<Map<String, Object>> lockedMissions = new ArrayList<>();

    public MissionListScreen() {
        super(Component.literal("Missions"));

        var player = Minecraft.getInstance().player;
        if (player != null) {
            this.uiContext = new UIContextImpl(player.getUUID());
            subscribeToMissions();
            EventUIFabricMod.LOGGER.info("Mission screen opened");
        } else {
            this.uiContext = null;
        }
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"),
                button -> this.onClose()
        ).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private void subscribeToMissions() {
        uiContext.subscribe("missions.available", this::onAvailableMissionsUpdate);
        uiContext.subscribe("missions.active", this::onActiveMissionsUpdate);
        uiContext.subscribe("missions.completed", this::onCompletedMissionsUpdate);
        uiContext.subscribe("missions.locked", this::onLockedMissionsUpdate);
    }

    @SuppressWarnings("unchecked")
    private void onAvailableMissionsUpdate(DataSnapshot snapshot) {
        List<Map<String, Object>> missions = (List<Map<String, Object>>) snapshot.get("missions");
        if (missions != null) {
            Minecraft.getInstance().execute(() -> {
                this.availableMissions = new ArrayList<>(missions);
                EventUIFabricMod.LOGGER.info("UI updated: {} available missions", missions.size());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void onActiveMissionsUpdate(DataSnapshot snapshot) {
        List<Map<String, Object>> missions = (List<Map<String, Object>>) snapshot.get("missions");
        if (missions != null) {
            Minecraft.getInstance().execute(() -> {
                this.activeMissions = new ArrayList<>(missions);
                EventUIFabricMod.LOGGER.info("UI updated: {} active missions", missions.size());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void onCompletedMissionsUpdate(DataSnapshot snapshot) {
        List<Map<String, Object>> missions = (List<Map<String, Object>>) snapshot.get("missions");
        if (missions != null) {
            Minecraft.getInstance().execute(() -> {
                this.completedMissions = new ArrayList<>(missions);
                EventUIFabricMod.LOGGER.info("UI updated: {} completed missions", missions.size());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void onLockedMissionsUpdate(DataSnapshot snapshot) {
        List<Map<String, Object>> missions = (List<Map<String, Object>>) snapshot.get("missions");
        if (missions != null) {
            Minecraft.getInstance().execute(() -> {
                this.lockedMissions = new ArrayList<>(missions);
                EventUIFabricMod.LOGGER.info("UI updated: {} locked missions", missions.size());
            });
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Vacío para no sobrescribir con blur
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Blur del mundo
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Título
        graphics.drawCenteredString(
                this.font,
                "MISSIONS",
                this.width / 2,
                20,
                0xFFFFFF
        );

        int yOffset = 60;

        // Misiones activas
        if (!activeMissions.isEmpty()) {
            graphics.drawString(
                    this.font,
                    "§e§lACTIVE:",
                    30,
                    yOffset,
                    0xFFFFFF
            );
            yOffset += 20;

            for (Map<String, Object> mission : activeMissions) {
                yOffset = renderMission(graphics, mission, yOffset, "active");
            }

            yOffset += 20;
        }

        // Misiones completadas
        if (!completedMissions.isEmpty()) {
            graphics.drawString(
                    this.font,
                    "§a§lCOMPLETED:",
                    30,
                    yOffset,
                    0xFFFFFF
            );
            yOffset += 20;

            for (Map<String, Object> mission : completedMissions) {
                yOffset = renderMission(graphics, mission, yOffset, "completed");
            }

            yOffset += 20;
        }

        // Misiones disponibles
        if (!availableMissions.isEmpty()) {
            graphics.drawString(
                    this.font,
                    "§a§lAVAILABLE:",
                    30,
                    yOffset,
                    0xFFFFFF
            );
            yOffset += 20;

            for (Map<String, Object> mission : availableMissions) {
                yOffset = renderMission(graphics, mission, yOffset, "available");
            }

            yOffset += 20;
        }

        // Misiones bloqueadas
        if (!lockedMissions.isEmpty()) {
            graphics.drawString(
                    this.font,
                    "§7§lLOCKED:",
                    30,
                    yOffset,
                    0xFFFFFF
            );
            yOffset += 20;

            for (Map<String, Object> mission : lockedMissions) {
                yOffset = renderMission(graphics, mission, yOffset, "locked");
            }
        }

        // No hay misiones
        if (availableMissions.isEmpty() && activeMissions.isEmpty()
                && completedMissions.isEmpty() && lockedMissions.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    "No missions found",
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFF
            );
        }

        // Botones
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int renderMission(GuiGraphics graphics, Map<String, Object> mission, int y, String state) {
        String title = (String) mission.get("title");
        String description = (String) mission.get("description");

        // Colores según estado
        String titleColor = switch (state) {
            case "active" -> "§e";
            case "completed" -> "§a";
            case "available" -> "§f";
            case "locked" -> "§8";
            default -> "§f";
        };

        String descColor = switch (state) {
            case "locked" -> "§8";
            case "completed" -> "§7";
            default -> "§7";
        };

        // Título
        graphics.drawString(
                this.font,
                titleColor + "• " + title,
                40,
                y,
                0xFFFFFF
        );
        y += 12;

        // Descripción
        graphics.drawString(
                this.font,
                descColor + "  " + description,
                40,
                y,
                0xFFFFFF
        );
        y += 12;

        // Estado específico
        switch (state) {
            case "locked" -> {
                graphics.drawString(
                        this.font,
                        "§8  [LOCKED]",
                        40,
                        y,
                        0xFFFFFF
                );
                y += 12;
            }
            case "completed" -> {
                graphics.drawString(
                        this.font,
                        "§a  ✓ [COMPLETED]",
                        40,
                        y,
                        0xFFFFFF
                );
                y += 12;
            }
            case "active" -> {
                // Progreso
                Object progress = mission.get("progress");
                Object target = mission.get("target");

                if (progress != null && target != null) {
                    graphics.drawString(
                            this.font,
                            "§6  Progress: " + progress + "/" + target,
                            40,
                            y,
                            0xFFFFFF
                    );
                    y += 12;
                }
            }
        }

        y += 10;
        return y;
    }

    @Override
    public void onClose() {
        if (uiContext != null) {
            uiContext.dispose();
            EventUIFabricMod.LOGGER.info("Mission screen closed");
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (uiContext != null) {
            uiContext.dispose();
        }
        super.removed();
    }
}
