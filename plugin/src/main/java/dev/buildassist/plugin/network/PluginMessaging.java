package dev.buildassist.plugin.network;

import dev.buildassist.plugin.db.StorageItem;
import dev.buildassist.plugin.storage.PlayerStorage;
import dev.buildassist.plugin.storage.StorageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
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

            // DB reads and writes on async thread; inventory changes back on main thread.
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                List<StorageItem> allItems = storage.getAll();
                int remaining = amount;

                record WithdrawnBatch(String nbt, int count) {}
                List<WithdrawnBatch> batches = new ArrayList<>();

                for (StorageItem si : allItems) {
                    if (remaining <= 0) break;
                    if (!si.getItemKey().equals(itemKey)) continue;
                    long take = Math.min(remaining, si.getCount());
                    if (!storage.withdraw(itemKey, si.getNbtData(), take)) continue;
                    batches.add(new WithdrawnBatch(si.getNbtData(), (int) take));
                    remaining -= (int) take;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (WithdrawnBatch b : batches) {
                        int left = b.count();
                        while (left > 0) {
                            int batch = Math.min(maxStack, left);
                            ItemStack item = reconstructItem(mat, batch, b.nbt());
                            if (shift) {
                                player.getInventory().addItem(item)
                                    .forEach((s, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                            } else {
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
                    }
                    player.updateInventory();
                    sendStorageUpdate(player, storage);
                });
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Withdraw error for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handleDeposit(Player player, String json) {
        try {
            String itemKey = extractJsonString(json, "item");
            int amount = extractJsonInt(json, "amount");
            if (itemKey == null || amount <= 0) return;

            PlayerStorage storage = storageManager.get(player);
            int remaining = amount;

            // Collect deposit entries on main thread (inventory mutation + NBT serialization).
            record DepositEntry(String key, String nbt, int count) {}
            List<DepositEntry> entries = new java.util.ArrayList<>();

            // Cursor item first (preserves NBT)
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()
                    && cursor.getType().getKey().toString().equals(itemKey)) {
                int take = Math.min(cursor.getAmount(), remaining);
                entries.add(new DepositEntry(itemKey, serializeItemNbt(cursor), take));
                int newAmt = cursor.getAmount() - take;
                if (newAmt <= 0) {
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                } else {
                    cursor.setAmount(newAmt);
                    player.setItemOnCursor(cursor);
                }
                remaining -= take;
            }

            // Player inventory slots
            if (remaining > 0) {
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length && remaining > 0; i++) {
                    ItemStack slot = contents[i];
                    if (slot == null || slot.getType().isAir()) continue;
                    if (!slot.getType().getKey().toString().equals(itemKey)) continue;
                    int take = Math.min(slot.getAmount(), remaining);
                    entries.add(new DepositEntry(itemKey, serializeItemNbt(slot), take));
                    int newAmt = slot.getAmount() - take;
                    if (newAmt <= 0) {
                        player.getInventory().setItem(i, new ItemStack(Material.AIR));
                    } else {
                        slot.setAmount(newAmt);
                    }
                    remaining -= take;
                }
            }

            // Open container (chest, barrel, etc.)
            if (remaining > 0) {
                org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
                ItemStack[] topContents = top.getContents();
                for (int i = 0; i < topContents.length && remaining > 0; i++) {
                    ItemStack slot = topContents[i];
                    if (slot == null || slot.getType().isAir()) continue;
                    if (!slot.getType().getKey().toString().equals(itemKey)) continue;
                    int take = Math.min(slot.getAmount(), remaining);
                    entries.add(new DepositEntry(itemKey, serializeItemNbt(slot), take));
                    int newAmt = slot.getAmount() - take;
                    if (newAmt <= 0) {
                        top.setItem(i, new ItemStack(Material.AIR));
                    } else {
                        slot.setAmount(newAmt);
                    }
                    remaining -= take;
                }
            }

            player.updateInventory();

            if (entries.isEmpty()) return;

            // DB writes on async thread to avoid blocking the server tick.
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                for (DepositEntry e : entries) {
                    storage.depositRaw(e.key(), e.nbt(), e.count());
                }
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sendStorageUpdate(player, storage));
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Deposit error for " + player.getName() + ": " + e.getMessage());
        }
    }

    private static String serializeItemNbt(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        org.bukkit.configuration.file.YamlConfiguration cfg =
            new org.bukkit.configuration.file.YamlConfiguration();
        cfg.set("i", item);
        return cfg.saveToString();
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
