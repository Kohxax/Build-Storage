package dev.buildassist.plugin.storage;

import dev.buildassist.plugin.db.StorageDatabase;
import dev.buildassist.plugin.db.StorageItem;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class PlayerStorage {

    private final UUID playerUuid;
    private final StorageDatabase database;

    public PlayerStorage(UUID playerUuid, StorageDatabase database) {
        this.playerUuid = playerUuid;
        this.database = database;
    }

    public List<StorageItem> getAll() {
        return database.getItems(playerUuid);
    }

    public long getCount(String itemKey, String nbtData) {
        return database.getCount(playerUuid, itemKey, nbtData);
    }

    public boolean deposit(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String key = item.getType().getKey().toString();
        String serialized = serializeItem(item);
        return database.adjustCount(playerUuid, key, serialized, item.getAmount());
    }

    public boolean depositRaw(String itemKey, String nbtData, int amount) {
        return database.adjustCount(playerUuid, itemKey, nbtData, amount);
    }

    public long getTotalCount(String itemKey) {
        return database.getTotalCount(playerUuid, itemKey);
    }

    // Returns false if insufficient stock.
    public boolean withdraw(String itemKey, String nbtData, long amount) {
        return database.adjustCount(playerUuid, itemKey, nbtData, -amount);
    }

    public String toJson() {
        List<StorageItem> items = getAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(items.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    private String serializeItem(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        org.bukkit.configuration.file.YamlConfiguration cfg =
            new org.bukkit.configuration.file.YamlConfiguration();
        cfg.set("i", item);
        return cfg.saveToString();
    }
}
