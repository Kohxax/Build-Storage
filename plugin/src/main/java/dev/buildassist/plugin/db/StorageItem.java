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
        String nbt     = nbtData != null ? "\"" + escapeJson(nbtData) + "\"" : "null";
        String display = buildDisplayJson();
        return "{\"item\":\"" + itemKey + "\",\"nbt\":" + nbt + ",\"count\":" + count + ",\"display\":" + display + "}";
    }

    private String buildDisplayJson() {
        if (nbtData == null) return "null";
        try {
            org.bukkit.configuration.file.YamlConfiguration cfg =
                new org.bukkit.configuration.file.YamlConfiguration();
            cfg.loadFromString(nbtData);
            org.bukkit.inventory.ItemStack item = cfg.getItemStack("i");
            if (item == null || !item.hasItemMeta()) return "null";
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

            StringBuilder sb = new StringBuilder("{");
            boolean comma = false;

            if (meta.hasDisplayName()) {
                sb.append("\"name\":\"").append(escapeJson(meta.getDisplayName())).append("\"");
                comma = true;
            }

            java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchants = meta.getEnchants();
            if (!enchants.isEmpty()) {
                if (comma) sb.append(",");
                sb.append("\"enchants\":[");
                boolean first = true;
                for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : enchants.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("{\"id\":\"").append(e.getKey().getKey().toString())
                      .append("\",\"level\":").append(e.getValue()).append("}");
                    first = false;
                }
                sb.append("]");
                comma = true;
            }

            if (meta.hasLore()) {
                java.util.List<String> lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    if (comma) sb.append(",");
                    sb.append("\"lore\":[");
                    boolean first = true;
                    for (String line : lore) {
                        if (!first) sb.append(",");
                        sb.append("\"").append(escapeJson(line)).append("\"");
                        first = false;
                    }
                    sb.append("]");
                }
            }

            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "null";
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
