package dev.buildassist.mod.client;

import java.util.List;

public class StorageEntry {

    public record EnchantEntry(String id, int level) {}

    private final String itemKey;
    private final String nbtData;
    private long count;
    private final String displayName;
    private final List<EnchantEntry> enchants;
    private final List<String> lore;

    public StorageEntry(String itemKey, String nbtData, long count,
                        String displayName, List<EnchantEntry> enchants, List<String> lore) {
        this.itemKey     = itemKey;
        this.nbtData     = nbtData;
        this.count       = count;
        this.displayName = displayName;
        this.enchants    = enchants != null ? List.copyOf(enchants) : List.of();
        this.lore        = lore    != null ? List.copyOf(lore)    : List.of();
    }

    public String getItemKey()         { return itemKey; }
    public String getNbtData()         { return nbtData; }
    public long   getCount()           { return count; }
    public void   setCount(long count) { this.count = count; }
    public String getDisplayName()     { return displayName; }
    public List<EnchantEntry> getEnchants() { return enchants; }
    public List<String> getLore()      { return lore; }

    public boolean hasDisplayData() {
        return displayName != null || !enchants.isEmpty() || !lore.isEmpty();
    }
}
