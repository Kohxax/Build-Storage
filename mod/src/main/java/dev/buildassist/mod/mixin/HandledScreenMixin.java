package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "removed()V", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if ((Object)this instanceof InventoryScreen) {
            BuildAssistClient.onInventoryClose();
        }
    }

    // Intercept shift+click in the survival inventory to deposit items into storage
    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!((Object)this instanceof InventoryScreen)) return;
        if (BuildAssistClient.getActivePanel() == null) return;
        if (actionType != SlotActionType.QUICK_MOVE) return;
        if (slot == null || slot.getStack().isEmpty()) return;

        ItemStack stack = slot.getStack();
        ModMessaging.sendDeposit(
            Registries.ITEM.getId(stack.getItem()).toString(),
            stack.getCount()
        );
        ci.cancel();
    }
}
