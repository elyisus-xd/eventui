package com.eventui.fabric.client.bridge;

import com.eventui.api.bridge.BridgeMessage;
import com.eventui.api.bridge.MessageType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Maneja el envío y recepción de paquetes de red entre MOD y PLUGIN.
 */
public class NetworkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);

    public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("eventui", "bridge");

    private final ClientEventBridge bridge;

    public NetworkHandler(ClientEventBridge bridge) {
        this.bridge = bridge;
        registerReceiver();
    }

    private void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(
                EventUIPayload.ID,
                (payload, context) -> {
                    byte[] data = payload.data();

                    context.client().execute(() -> {
                        try {
                            BridgeMessage message = deserializeMessage(data);
                            bridge.handleIncomingMessage(message);
                        } catch (Exception e) {
                            LOGGER.error("Failed to process incoming message", e);
                        }
                    });
                }
        );

        LOGGER.info("Network receiver registered on channel: {}", CHANNEL_ID);
    }

    public CompletableFuture<Void> sendMessage(BridgeMessage message) {
        try {
            byte[] data = serializeMessage(message);

            EventUIPayload payload = new EventUIPayload(data);
            ClientPlayNetworking.send(payload);

            LOGGER.debug("Sent message type: {} ({} bytes)", message.getType(), data.length);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            LOGGER.error("Failed to send message", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private byte[] serializeMessage(BridgeMessage message) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try (DataOutputStream out = new DataOutputStream(byteOut)) {
            out.writeByte(message.getType().ordinal());

            Map<String, String> payload = message.getPayload();
            out.writeInt(payload.size());

            for (Map.Entry<String, String> entry : payload.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }

            out.flush();
            return byteOut.toByteArray();

        }
    }

    private BridgeMessage deserializeMessage(byte[] data) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);

        try (DataInputStream in = new DataInputStream(byteIn)) {
            MessageType type = MessageType.values()[in.readByte()];

            int payloadSize = in.readInt();
            Map<String, String> payload = new HashMap<>();

            for (int i = 0; i < payloadSize; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                payload.put(key, value);
            }

            UUID localPlayerId = net.minecraft.client.Minecraft.getInstance().player != null
                    ? net.minecraft.client.Minecraft.getInstance().player.getUUID()
                    : null;

            return new BridgeMessageImpl(
                    type,
                    payload,
                    localPlayerId,
                    System.currentTimeMillis(),
                    null,
                    null
            );

        }
    }

    public record EventUIPayload(byte[] data) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<EventUIPayload> ID =
                new CustomPacketPayload.Type<>(CHANNEL_ID);

        public static final StreamCodec<FriendlyByteBuf, EventUIPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeBytes(payload.data),
                        buf -> {
                            byte[] data = new byte[buf.readableBytes()];
                            buf.readBytes(data);
                            return new EventUIPayload(data);
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void registerPayloadType() {
        PayloadTypeRegistry.playS2C().register(EventUIPayload.ID, EventUIPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EventUIPayload.ID, EventUIPayload.CODEC);

        LOGGER.info("Registered EventUI payload type");
    }
}
