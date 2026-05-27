package dev.buildassist.mod.client.screen;

import dev.buildassist.mod.client.StorageCache;
import dev.buildassist.mod.client.StorageEntry;
import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class StoragePanelHandler {

    public static class SlotEntry {
        public final ItemStack displayStack;
        public final String itemKey;
        public final long count;
        public final String nbtData;

        public SlotEntry(ItemStack displayStack, String itemKey, long count) {
            this(displayStack, itemKey, count, null);
        }

        public SlotEntry(ItemStack displayStack, String itemKey, long count, String nbtData) {
            this.displayStack = displayStack;
            this.itemKey      = itemKey;
            this.count        = count;
            this.nbtData      = nbtData;
        }

        public boolean isOwned()  { return count > 0; }
        public boolean hasNbt()   { return nbtData != null; }
    }

    private final StorageCache cache;

    public StoragePanelHandler(StorageCache cache) {
        this.cache = cache;
    }

    // Returns all items in the current creative tab, filtered by search, with count from cache.
    // Uses getCountAll so enchanted variants are included in the total.
    public List<SlotEntry> buildSlots(List<ItemStack> tabItems, String searchQuery) {
        List<SlotEntry> result = new ArrayList<>();
        String lowerSearch = searchQuery == null ? "" : searchQuery.toLowerCase();

        for (ItemStack stack : tabItems) {
            if (stack.isEmpty()) continue;
            Identifier id = Registries.ITEM.getId(stack.getItem());
            String itemKey = id.toString();
            String displayName = stack.getName().getString().toLowerCase();

            if (!lowerSearch.isEmpty() && !displayName.contains(lowerSearch) && !itemKey.contains(lowerSearch)) {
                continue;
            }

            long count = cache.getCount(itemKey);
            result.add(new SlotEntry(stack, itemKey, count));
        }
        return result;
    }

    public void withdraw(String itemKey, int amount, boolean shift) {
        ModMessaging.sendWithdraw(itemKey, amount, shift);
    }

    public void depositHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) return;
        ModMessaging.sendDeposit(Registries.ITEM.getId(held.getItem()).toString(), held.getCount());
    }
}
