package dev.buildassist.mod.client;

import com.google.gson.*;
import dev.buildassist.mod.BuildAssistMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StorageCache {

    public static final StorageCache INSTANCE = new StorageCache();

    // itemKey -> StorageEntry  (nbt-less items use null nbt key)
    private final Map<String, StorageEntry> entries = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private StorageCache() {}

    public void update(String json) {
        try {
            entries.clear();
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                String itemKey = obj.get("item").getAsString();
                String nbtData = obj.has("nbt") && !obj.get("nbt").isJsonNull()
                    ? obj.get("nbt").getAsString() : null;
                long count = obj.get("count").getAsLong();
                entries.put(cacheKey(itemKey, nbtData), new StorageEntry(itemKey, nbtData, count));
            }
            notifyListeners();
        } catch (Exception e) {
            BuildAssistMod.LOGGER.error("Failed to parse storage JSON", e);
        }
    }

    public long getCount(String itemKey) {
        StorageEntry entry = entries.get(cacheKey(itemKey, null));
        return entry != null ? entry.getCount() : 0;
    }

    public Collection<StorageEntry> getAll() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public void clear() {
        entries.clear();
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    private static String cacheKey(String itemKey, String nbtData) {
        return nbtData == null ? itemKey : itemKey + "@" + nbtData;
    }
}
