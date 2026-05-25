package dev.buildassist.mod.network;

import dev.buildassist.mod.BuildAssistMod;
import dev.buildassist.mod.client.StorageCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.*;

public class ModMessaging {

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(StoragePayload.ID, (payload, context) -> {
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload.data()));
                String packetId = in.readUTF();
                String json = in.readUTF();
                context.client().execute(() -> handlePacket(packetId, json));
            } catch (IOException e) {
                BuildAssistMod.LOGGER.error("Failed to read server storage packet", e);
            }
        });
    }

    public static void sendOpenStorage() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeUTF("open_storage");
            ClientPlayNetworking.send(new StoragePayload(buf.toByteArray()));
        } catch (IOException e) {
            BuildAssistMod.LOGGER.error("Failed to send open_storage packet", e);
        }
    }

    private static void handlePacket(String packetId, String json) {
        switch (packetId) {
            case "storage_contents", "storage_update" -> StorageCache.INSTANCE.update(json);
        }
    }
}
