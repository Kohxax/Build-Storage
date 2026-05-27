package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
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

    // Intercept slot actions in the survival inventory when the storage panel is open
    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (!((Object)this instanceof InventoryScreen)) return;
        if (BuildAssistClient.getActivePanel() == null) return;

        // Prevent cursor items from being dropped when clicking outside all slots —
        // deposit them into storage instead (fixes issue 19: withdraw then drop)
        if (slot == null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
                if (!cursor.isEmpty()) {
                    ModMessaging.sendDeposit(
                        Registries.ITEM.getId(cursor.getItem()).toString(),
                        cursor.getCount()
                    );
                }
            }
            ci.cancel();
            return;
        }

        // Ctrl+left-click: deposit the clicked slot's item into storage
        // Plain Shift+click is left as vanilla (hotbar ↔ inventory move)
        if (button != 0 || actionType != SlotActionType.PICKUP) return;
        if (slot.getStack().isEmpty()) return;

        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (!ctrl) return;

        ItemStack stack = slot.getStack();
        ModMessaging.sendDeposit(
            Registries.ITEM.getId(stack.getItem()).toString(),
            stack.getCount()
        );
        ci.cancel();
    }

}
