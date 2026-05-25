package dev.buildassist.mod.client;

public class StorageEntry {

    private final String itemKey;
    private final String nbtData;
    private long count;

    public StorageEntry(String itemKey, String nbtData, long count) {
        this.itemKey = itemKey;
        this.nbtData = nbtData;
        this.count = count;
    }

    public String getItemKey() { return itemKey; }
    public String getNbtData() { return nbtData; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
