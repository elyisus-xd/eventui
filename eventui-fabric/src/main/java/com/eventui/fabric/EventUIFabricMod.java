package com.eventui.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUIFabricMod implements ModInitializer {
    public static final String MOD_ID = "eventui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("EventUI mod initialized!");
    }
}
