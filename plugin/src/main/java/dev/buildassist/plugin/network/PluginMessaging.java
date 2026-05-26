package dev.buildassist.plugin.network;

import dev.buildassist.plugin.db.StorageItem;
import dev.buildassist.plugin.storage.PlayerStorage;
import dev.buildassist.plugin.storage.StorageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

import java.io.*;

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
            String json = in.readUTF();

            switch (packetId) {
                case "open_storage" -> handleOpenStorage(player);
                case "withdraw"     -> handleWithdraw(player, json);
                case "deposit"      -> handleDeposit(player, json);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read plugin message from " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handleOpenStorage(Player player) {
        PlayerStorage storage = storageManager.get(player);
        sendStorageContents(player, storage);
    }

    private void handleWithdraw(Player player, String json) {
        try {
            String itemKey = extractJsonString(json, "item");
            int amount = extractJsonInt(json, "amount");
            boolean shift = extractJsonBoolean(json, "shift");
            if (itemKey == null || amount <= 0) return;

            PlayerStorage storage = storageManager.get(player);
            Material mat = Material.matchMaterial(itemKey);
            if (mat == null) return;

            int maxStack = mat.getMaxStackSize();
            int remaining = amount;

            // Iterate all storage entries for this item (covers all NBT variants)
            List<StorageItem> allItems = storage.getAll();
            for (StorageItem si : allItems) {
                if (remaining <= 0) break;
                if (!si.getItemKey().equals(itemKey)) continue;

                long take = Math.min(remaining, si.getCount());
                if (!storage.withdraw(itemKey, si.getNbtData(), take)) continue;

                int left = (int) take;
                while (left > 0) {
                    int batch = Math.min(maxStack, left);
                    ItemStack item = reconstructItem(mat, batch, si.getNbtData());
                    if (shift) {
                        player.getInventory().addItem(item)
                            .forEach((s, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    } else {
                        // Place on cursor like vanilla left-click
                        ItemStack cursor = player.getItemOnCursor();
                        if (cursor == null || cursor.getType().isAir()) {
                            player.setItemOnCursor(item);
                        } else if (cursor.isSimilar(item) && cursor.getAmount() + batch <= maxStack) {
                            cursor.setAmount(cursor.getAmount() + batch);
                            player.setItemOnCursor(cursor);
                        } else {
                            player.getInventory().addItem(item)
                                .forEach((s, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                        }
                    }
                    left -= batch;
                }
                remaining -= (int) take;
            }
            sendStorageUpdate(player, storage);
        } catch (Exception e) {
            plugin.getLogger().warning("Withdraw error for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handleDeposit(Player player, String json) {
        try {
            String itemKey = extractJsonString(json, "item");
            int amount = extractJsonInt(json, "amount");
            if (itemKey == null || amount <= 0) return;

            Material mat = Material.matchMaterial(itemKey);
            if (mat == null) return;

            PlayerStorage storage = storageManager.get(player);
            ItemStack depositItem = new ItemStack(mat, amount);
            if (storage.deposit(depositItem)) {
                // Remove from cursor first (panel deposits cursor items), then from inventory
                int remaining = amount;
                ItemStack cursor = player.getItemOnCursor();
                if (cursor != null && !cursor.getType().isAir()
                        && cursor.getType().getKey().toString().equals(itemKey)) {
                    int take = Math.min(cursor.getAmount(), remaining);
                    int newCursorAmount = cursor.getAmount() - take;
                    if (newCursorAmount <= 0) {
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    } else {
                        cursor.setAmount(newCursorAmount);
                        player.setItemOnCursor(cursor);
                    }
                    remaining -= take;
                }
                if (remaining > 0) {
                    player.getInventory().removeItem(new ItemStack(mat, remaining));
                }
                sendStorageUpdate(player, storage);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Deposit error for " + player.getName() + ": " + e.getMessage());
        }
    }

    private ItemStack reconstructItem(Material mat, int amount, String nbtData) {
        if (nbtData != null) {
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    new org.bukkit.configuration.file.YamlConfiguration();
                cfg.loadFromString(nbtData);
                ItemStack template = cfg.getItemStack("i");
                if (template != null) {
                    template.setAmount(amount);
                    return template;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deserialize item NBT, using plain item: " + e.getMessage());
            }
        }
        return new ItemStack(mat, amount);
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

    // Minimal JSON field extraction without external dependencies
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? null : json.substring(start, end);
    }

    private static int extractJsonInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return 0;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }

    private static boolean extractJsonBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return false;
        start += search.length();
        return json.startsWith("true", start);
    }
}
