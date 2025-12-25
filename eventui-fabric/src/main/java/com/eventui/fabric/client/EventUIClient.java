package com.eventui.fabric.client;

import com.eventui.fabric.client.bridge.ClientEventBridge;
import com.eventui.fabric.client.bridge.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUIClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("EventUI-Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing EventUI client...");

        // Registrar payload type
        NetworkHandler.registerPayloadType();

        // Inicializar bridge
        ClientEventBridge.getInstance();

        LOGGER.info("EventUI client initialized!");
    }
}
