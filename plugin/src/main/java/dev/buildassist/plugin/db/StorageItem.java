package dev.buildassist.plugin.db;

public class StorageItem {

    private final String itemKey;
    private final String nbtData;
    private long count;

    public StorageItem(String itemKey, String nbtData, long count) {
        this.itemKey = itemKey;
        this.nbtData = nbtData;
        this.count = count;
    }

    public String getItemKey() { return itemKey; }
    public String getNbtData() { return nbtData; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public String toJson() {
        String nbt = nbtData != null ? "\"" + nbtData.replace("\"", "\\\"") + "\"" : "null";
        return "{\"item\":\"" + itemKey + "\",\"nbt\":" + nbt + ",\"count\":" + count + "}";
    }
}
