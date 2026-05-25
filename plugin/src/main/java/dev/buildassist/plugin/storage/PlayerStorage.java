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
        String nbt = serializeNbt(item);
        return database.adjustCount(playerUuid, key, nbt, item.getAmount());
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

    private String serializeNbt(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        // Serialize ItemMeta to a stable string key using Bukkit's config serialization.
        // This is sufficient for distinguishing enchanted/named items in storage.
        return item.getItemMeta().getAsString();
    }
}
