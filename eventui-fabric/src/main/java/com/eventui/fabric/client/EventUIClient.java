package com.eventui.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import com.eventui.fabric.EventUIFabricMod;

public class EventUIClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EventUIFabricMod.LOGGER.info("EventUI client initialized!");
    }
}
