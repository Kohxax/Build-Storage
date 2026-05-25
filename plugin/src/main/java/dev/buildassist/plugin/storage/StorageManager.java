package dev.buildassist.plugin.storage;

import dev.buildassist.plugin.db.StorageDatabase;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageManager {

    private final StorageDatabase database;
    private final Map<UUID, PlayerStorage> cache = new HashMap<>();

    public StorageManager(StorageDatabase database) {
        this.database = database;
    }

    public PlayerStorage get(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStorage(uuid, database));
    }

    public PlayerStorage get(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> new PlayerStorage(u, database));
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
}
