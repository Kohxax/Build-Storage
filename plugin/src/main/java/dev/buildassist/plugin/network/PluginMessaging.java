package dev.buildassist.plugin.network;

import dev.buildassist.plugin.menu.StorageMenu;
import dev.buildassist.plugin.storage.PlayerStorage;
import dev.buildassist.plugin.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PluginMessaging implements PluginMessageListener {

    public static final String CHANNEL = "buildassist:main";

    private final Plugin plugin;
    private final StorageManager storageManager;

    public PluginMessaging(Plugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
    }

    public void register() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String packetId = in.readUTF();
            if ("open_storage".equals(packetId)) {
                PlayerStorage storage = storageManager.get(player);
                StorageMenu menu = new StorageMenu(storage);
                menu.open(player);
                sendStorageContents(player, storage);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read plugin message from " + player.getName() + ": " + e.getMessage());
        }
    }

    public void sendStorageContents(Player player, PlayerStorage storage) {
        send(player, "storage_contents", storage.toJson());
    }

    public void sendStorageUpdate(Player player, PlayerStorage storage) {
        send(player, "storage_update", storage.toJson());
    }

    private void send(Player player, String packetId, String json) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeUTF(packetId);
            out.writeUTF(json);
            player.sendPluginMessage(plugin, CHANNEL, buf.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send plugin message: " + e.getMessage());
        }
    }
}
