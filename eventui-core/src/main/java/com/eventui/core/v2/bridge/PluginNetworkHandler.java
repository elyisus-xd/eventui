package com.eventui.core.v2.bridge;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.bridge.MessageType;
import com.eventui.core.v2.EventUIPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Maneja la comunicación de red del lado del plugin (Paper).*
 * ARQUITECTURA:
 * - Usa Bukkit Plugin Messaging API
 * - Serialización binaria con DataOutputStream/DataInputStream
 * - Recibe mensajes del MOD y envía respuestas
 */
public class PluginNetworkHandler implements PluginMessageListener {

    private static final Logger LOGGER = Logger.getLogger(PluginNetworkHandler.class.getName());
    private static final String CHANNEL = "eventui:bridge";

    private final PluginEventBridge bridge;
    private final EventUIPlugin plugin;

    public PluginNetworkHandler(PluginEventBridge bridge, EventUIPlugin plugin) {
        this.bridge = bridge;
        this.plugin = plugin;

        registerChannel();
    }

    /**
     * Registra el canal de comunicación con Bukkit.
     */
    private void registerChannel() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);

        LOGGER.info("Registered plugin messaging channel: " + CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] messageData) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        try {
            BridgeMessage bridgeMessage = deserializeMessage(messageData);

            // Asignar player del contexto
            BridgeMessage messageWithPlayer = new PluginBridgeMessage(
                    bridgeMessage.getType(),
                    bridgeMessage.getPayload(),
                    player.getUniqueId(),
                    bridgeMessage.getTimestamp(),
                    bridgeMessage.getMessageId(),
                    bridgeMessage.getReplyToMessageId()
            );

            bridge.handleIncomingMessage(messageWithPlayer, player);

        } catch (Exception e) {
            LOGGER.severe("Failed to process plugin message from " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Envía un mensaje a un jugador específico.
     */
    public CompletableFuture<Void> sendMessage(BridgeMessage message) {
        try {
            Player player = plugin.getServer().getPlayer(message.getPlayerId());

            if (player == null || !player.isOnline()) {
                LOGGER.warning("Cannot send message, player offline: " + message.getPlayerId());
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Player offline")
                );
            }

            byte[] data = serializeMessage(message);

            player.sendPluginMessage(plugin, CHANNEL, data);

            LOGGER.fine("Sent message type " + message.getType() + " to " + player.getName() +
                    " (" + data.length + " bytes)");

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            LOGGER.severe("Failed to send plugin message");
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Serializa un BridgeMessage a bytes usando DataOutputStream.
     */
    private byte[] serializeMessage(BridgeMessage message) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try (DataOutputStream out = new DataOutputStream(byteOut)) {
            // Escribir tipo de mensaje (1 byte)
            out.writeByte(message.getType().ordinal());

            // Escribir payload
            Map<String, String> payload = message.getPayload();
            out.writeInt(payload.size()); // Número de entries

            for (Map.Entry<String, String> entry : payload.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }

            out.flush();
            return byteOut.toByteArray();

        }
    }

    /**
     * Deserializa bytes a BridgeMessage usando DataInputStream.
     */
    private BridgeMessage deserializeMessage(byte[] data) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);

        try (DataInputStream in = new DataInputStream(byteIn)) {
            // Leer tipo de mensaje
            MessageType type = MessageType.values()[in.readByte()];

            // Leer payload
            int payloadSize = in.readInt();
            Map<String, String> payload = new HashMap<>();

            for (int i = 0; i < payloadSize; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                payload.put(key, value);
            }

            return new PluginBridgeMessage(
                    type,
                    payload,
                    null, // Se asigna desde contexto
                    System.currentTimeMillis(),
                    UUID.randomUUID(),
                    null
            );

        }
    }

    /**
     * Desregistra el canal al deshabilitar el plugin.
     */
    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);

        LOGGER.info("Unregistered plugin messaging channel");
    }
}
