package dev.buildassist.plugin.menu;

import dev.buildassist.plugin.db.StorageItem;
import dev.buildassist.plugin.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StorageMenu implements InventoryHolder {

    public static final String TITLE = "§8統合倉庫";
    public static final int ROWS = 6;
    public static final int SIZE = ROWS * 9;

    private final PlayerStorage storage;
    private final Inventory inventory;

    public StorageMenu(PlayerStorage storage) {
        this.storage = storage;
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
        refresh();
    }

    public void refresh() {
        inventory.clear();
        List<StorageItem> items = storage.getAll();
        int slot = 0;
        for (StorageItem si : items) {
            if (slot >= SIZE) break;
            Material mat = Material.matchMaterial(si.getItemKey());
            if (mat == null || mat.isAir()) continue;
            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("§f" + formatName(mat));
            meta.setLore(buildLore(si.getCount()));
            stack.setItemMeta(meta);
            inventory.setItem(slot++, stack);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public PlayerStorage getStorage() {
        return storage;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private String formatName(Material mat) {
        String name = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    static List<String> buildLore(long count) {
        List<String> lore = new ArrayList<>();
        lore.add("§7在庫: §e" + formatCount(count));
        lore.add("§7左クリック: §f64個取り出す");
        lore.add("§7右クリック: §f32個取り出す");
        lore.add("§7Shift+クリック: §fスタック全取り出し");
        return lore;
    }

    public static String formatCount(long count) {
        if (count >= 1_000_000) return String.format("%.1fm", count / 1_000_000.0);
        if (count >= 1_000) return String.format("%.1fk", count / 1_000.0);
        return String.valueOf(count);
    }
}
