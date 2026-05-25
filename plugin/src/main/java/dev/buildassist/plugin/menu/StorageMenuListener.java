package dev.buildassist.plugin.menu;

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

        event.setCancelled(true);

        ClickType click = event.getClick();
        Inventory topInv = event.getInventory();
        boolean inTopInv = event.getRawSlot() < topInv.getSize();

        PlayerStorage storage = menu.getStorage();

        // Deposit: shift-click an item from the player's own inventory
        if (!inTopInv && click == ClickType.SHIFT_LEFT) {
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            if (storage.deposit(item.clone())) {
                player.getInventory().setItem(event.getSlot(), null);
                menu.refresh();
                notifyUpdate(player, storage);
            }
            return;
        }

        if (!inTopInv) return;

        ItemStack slotItem = event.getCurrentItem();
        if (slotItem == null || slotItem.getType().isAir()) return;

        Material mat = slotItem.getType();
        String itemKey = mat.getKey().toString();
        long inStorage = storage.getCount(itemKey, null);

        switch (click) {
            case LEFT         -> withdraw(player, storage, menu, itemKey, mat, Math.min(mat.getMaxStackSize(), inStorage));
            case RIGHT        -> withdraw(player, storage, menu, itemKey, mat, Math.min(Math.max(1, mat.getMaxStackSize() / 2), inStorage));
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

    private void withdraw(Player player, PlayerStorage storage, StorageMenu menu,
                          String itemKey, Material mat, long amount) {
        if (amount <= 0) return;
        int maxStack = mat.getMaxStackSize();
        long remaining = amount;
        while (remaining > 0) {
            int batch = (int) Math.min(maxStack, remaining);
            if (!storage.withdraw(itemKey, null, batch)) break;
            player.getInventory().addItem(new ItemStack(mat, batch))
                .forEach((s, leftover) -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            remaining -= batch;
        }
        menu.refresh();
        notifyUpdate(player, storage);
    }

    private void collectToHand(Player player, PlayerStorage storage, StorageMenu menu,
                                String itemKey, Material mat) {
        long inStorage = storage.getCount(itemKey, null);
        long take = Math.min(mat.getMaxStackSize(), inStorage);
        if (take <= 0) return;
        if (storage.withdraw(itemKey, null, take)) {
            player.setItemOnCursor(new ItemStack(mat, (int) take));
            menu.refresh();
            notifyUpdate(player, storage);
        }
    }

    private void swapWithHotbar(Player player, PlayerStorage storage, StorageMenu menu,
                                 String itemKey, Material mat, int hotbarSlot) {
        ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
        if (hotbarItem != null && !hotbarItem.getType().isAir()) {
            storage.deposit(hotbarItem.clone());
            player.getInventory().setItem(hotbarSlot, null);
        }
        long inStorage = storage.getCount(itemKey, null);
        int take = (int) Math.min(mat.getMaxStackSize(), inStorage);
        if (take > 0 && storage.withdraw(itemKey, null, take)) {
            player.getInventory().setItem(hotbarSlot, new ItemStack(mat, take));
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
