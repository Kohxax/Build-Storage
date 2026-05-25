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

    // Represents one slot in the panel: a vanilla item + its storage count
    public static class SlotEntry {
        public final ItemStack displayStack;
        public final String itemKey;
        public final long count;

        public SlotEntry(ItemStack displayStack, String itemKey, long count) {
            this.displayStack = displayStack;
            this.itemKey = itemKey;
            this.count = count;
        }

        public boolean isOwned() {
            return count > 0;
        }
    }

    private final StorageCache cache;

    public StoragePanelHandler(StorageCache cache) {
        this.cache = cache;
    }

    // Returns all items in the current creative tab, filtered by search, with count from cache.
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

    // Withdraw one stack from the server storage
    public void withdraw(String itemKey, int amount) {
        // Send withdraw request via plugin messaging
        // The actual packet is open_storage for now; a dedicated withdraw packet
        // will be added when the server-side supports it.
        // For Phase 3, we defer to the server's open_storage+click flow.
        // TODO: add withdraw packet in Phase 5 integration
        ModMessaging.sendWithdraw(itemKey, amount);
    }

    // Deposit the player's current held item into storage
    public void depositHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) return;
        ModMessaging.sendDeposit(Registries.ITEM.getId(held.getItem()).toString(), held.getCount());
    }
}
