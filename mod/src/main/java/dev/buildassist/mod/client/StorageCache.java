package dev.buildassist.mod.client;

import com.google.gson.*;
import dev.buildassist.mod.BuildAssistMod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StorageCache {

    public static final StorageCache INSTANCE = new StorageCache();

    private final Map<String, StorageEntry> entries = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

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

                String displayName = null;
                List<StorageEntry.EnchantEntry> enchants = new ArrayList<>();
                List<String> lore = new ArrayList<>();

                if (obj.has("display") && !obj.get("display").isJsonNull()) {
                    JsonObject display = obj.getAsJsonObject("display");
                    if (display.has("name")) {
                        displayName = display.get("name").getAsString();
                    }
                    if (display.has("enchants")) {
                        for (JsonElement e : display.getAsJsonArray("enchants")) {
                            JsonObject ench = e.getAsJsonObject();
                            enchants.add(new StorageEntry.EnchantEntry(
                                ench.get("id").getAsString(),
                                ench.get("level").getAsInt()
                            ));
                        }
                    }
                    if (display.has("lore")) {
                        for (JsonElement l : display.getAsJsonArray("lore")) {
                            lore.add(l.getAsString());
                        }
                    }
                }

                entries.put(cacheKey(itemKey, nbtData),
                    new StorageEntry(itemKey, nbtData, count, displayName, enchants, lore));
            }
            notifyListeners();
        } catch (Exception e) {
            BuildAssistMod.LOGGER.error("Failed to parse storage JSON", e);
        }
    }

    public long getCount(String itemKey) {
        return entries.values().stream()
            .filter(e -> e.getItemKey().equals(itemKey) && e.getNbtData() == null)
            .mapToLong(StorageEntry::getCount)
            .sum();
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
