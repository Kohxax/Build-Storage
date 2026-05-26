package dev.buildassist.plugin.menu;

import dev.buildassist.plugin.db.StorageItem;
import dev.buildassist.plugin.network.PluginMessaging;
import dev.buildassist.plugin.storage.PlayerStorage;
import dev.buildassist.plugin.storage.StorageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StorageMenuListener implements Listener {

    private final StorageManager storageManager;
    private PluginMessaging messaging;

    public StorageMenuListener(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void setMessaging(PluginMessaging messaging) {
        this.messaging = messaging;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof StorageMenu menu)) return;

        ClickType click = event.getClick();
        Inventory topInv = event.getInventory();
        boolean inTopInv = event.getRawSlot() < topInv.getSize();

        PlayerStorage storage = menu.getStorage();

        // Deposit: shift-click an item from the player's own inventory
        if (!inTopInv && click == ClickType.SHIFT_LEFT) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            if (storage.deposit(item.clone())) {
                player.getInventory().setItem(event.getSlot(), null);
                menu.refresh();
                notifyUpdate(player, storage);
            }
            return;
        }

        // Allow all other operations in the player's bottom inventory
        if (!inTopInv) return;

        // Top inventory (storage display) — cancel and handle
        event.setCancelled(true);

        ItemStack slotItem = event.getCurrentItem();
        if (slotItem == null || slotItem.getType().isAir()) return;

        Material mat = slotItem.getType();
        String itemKey = mat.getKey().toString();
        long inStorage = storage.getTotalCount(itemKey);

        switch (click) {
            case LEFT         -> withdraw(player, storage, menu, itemKey, mat, Math.min(mat.getMaxStackSize(), inStorage));
            case RIGHT        -> withdraw(player, storage, menu, itemKey, mat, Math.min(1, inStorage));
            case SHIFT_LEFT, SHIFT_RIGHT -> withdraw(player, storage, menu, itemKey, mat, inStorage);
            case DOUBLE_CLICK -> collectToHand(player, storage, menu, itemKey, mat);
            case NUMBER_KEY   -> swapWithHotbar(player, storage, menu, itemKey, mat, event.getHotbarButton());
            case DROP         -> withdraw(player, storage, menu, itemKey, mat, Math.min(1, inStorage));
            case CONTROL_DROP -> withdraw(player, storage, menu, itemKey, mat, Math.min(mat.getMaxStackSize(), inStorage));
            default           -> { /* middle click and others: no-op */ }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageMenu)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageMenu)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            player.getInventory().addItem(cursor).forEach((slot, leftover) ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            player.setItemOnCursor(null);
        }
    }

    // --- helpers ---

    private ItemStack reconstructItem(Material mat, int amount, String nbtData) {
        if (nbtData != null) {
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    new org.bukkit.configuration.file.YamlConfiguration();
                cfg.loadFromString(nbtData);
                ItemStack template = cfg.getItemStack("i");
                if (template != null) {
                    template.setAmount(amount);
                    return template;
                }
            } catch (Exception ignored) {}
        }
        return new ItemStack(mat, amount);
    }

    private void withdraw(Player player, PlayerStorage storage, StorageMenu menu,
                          String itemKey, Material mat, long amount) {
        if (amount <= 0) return;
        int maxStack = mat.getMaxStackSize();
        long remaining = amount;
        for (StorageItem si : storage.getAll()) {
            if (remaining <= 0) break;
            if (!si.getItemKey().equals(itemKey)) continue;
            long take = Math.min(remaining, si.getCount());
            if (!storage.withdraw(itemKey, si.getNbtData(), take)) continue;
            int left = (int) take;
            while (left > 0) {
                int batch = Math.min(maxStack, left);
                player.getInventory().addItem(reconstructItem(mat, batch, si.getNbtData()))
                    .forEach((s, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                left -= batch;
            }
            remaining -= (int) take;
        }
        menu.refresh();
        notifyUpdate(player, storage);
    }

    private void collectToHand(Player player, PlayerStorage storage, StorageMenu menu,
                                String itemKey, Material mat) {
        long inStorage = storage.getTotalCount(itemKey);
        long take = Math.min(mat.getMaxStackSize(), inStorage);
        if (take <= 0) return;
        for (StorageItem si : storage.getAll()) {
            if (!si.getItemKey().equals(itemKey)) continue;
            long actualTake = Math.min(take, si.getCount());
            if (storage.withdraw(itemKey, si.getNbtData(), actualTake)) {
                player.setItemOnCursor(reconstructItem(mat, (int) actualTake, si.getNbtData()));
                menu.refresh();
                notifyUpdate(player, storage);
                return;
            }
        }
    }

    private void swapWithHotbar(Player player, PlayerStorage storage, StorageMenu menu,
                                 String itemKey, Material mat, int hotbarSlot) {
        ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
        if (hotbarItem != null && !hotbarItem.getType().isAir()) {
            storage.deposit(hotbarItem.clone());
            player.getInventory().setItem(hotbarSlot, null);
        }
        long inStorage = storage.getTotalCount(itemKey);
        int take = (int) Math.min(mat.getMaxStackSize(), inStorage);
        if (take > 0) {
            for (StorageItem si : storage.getAll()) {
                if (!si.getItemKey().equals(itemKey)) continue;
                int actualTake = (int) Math.min(take, si.getCount());
                if (storage.withdraw(itemKey, si.getNbtData(), actualTake)) {
                    player.getInventory().setItem(hotbarSlot, reconstructItem(mat, actualTake, si.getNbtData()));
                    break;
                }
            }
        }
        menu.refresh();
        notifyUpdate(player, storage);
    }

    private void notifyUpdate(Player player, PlayerStorage storage) {
        if (messaging != null) {
            messaging.sendStorageUpdate(player, storage);
        }
    }
}
