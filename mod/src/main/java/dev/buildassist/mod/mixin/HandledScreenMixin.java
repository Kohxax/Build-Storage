package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.render.ItemCountRenderer;
import dev.buildassist.mod.client.screen.StoragePanel;
import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Shadow
    protected Slot getSlotAt(double x, double y) { return null; }

    @Unique private boolean baCtrlDragging = false;
    @Unique private final Set<Integer> baCtrlDraggedSlots = new HashSet<>();

    @Inject(method = "removed()V", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        BuildAssistClient.onInventoryClose();
    }

    // Open panel for GenericContainerScreen (chest, ender chest, barrel, etc.)
    // InventoryScreen is handled by InventoryScreenMixin.
    @Inject(method = "init", at = @At("TAIL"))
    private void onHandledInit(CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen) return;
        if ((Object) this instanceof GenericContainerScreen) {
            BuildAssistClient.onInventoryOpen((HandledScreen)(Object) this);
        }
    }

    // Render panel for non-InventoryScreen handled screens.
    // InventoryScreen is handled by InventoryScreenMixin.
    @Inject(method = "render", at = @At("TAIL"))
    private void onHandledRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null) {
            panel.render(context, mouseX, mouseY, delta);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                ItemStack cursorStack = mc.player.currentScreenHandler.getCursorStack();
                if (!cursorStack.isEmpty()) {
                    context.drawItem(cursorStack, mouseX - 8, mouseY - 8);
                    ItemCountRenderer.render(context, cursorStack.getCount(), mouseX - 8, mouseY - 8);
                }
            }
        }
    }

    // Start Ctrl+drag session when Ctrl+LMB is pressed with panel open.
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onCtrlDragStart(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || BuildAssistClient.getActivePanel() == null) return;
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (ctrl) {
            baCtrlDragging = true;
            baCtrlDraggedSlots.clear();
        }
    }

    // Deposit each new player-inventory slot entered during Ctrl+drag.
    @Inject(method = "mouseDragged", at = @At("HEAD"))
    private void onCtrlDrag(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (!baCtrlDragging || click.button() != 0) return;
        if (BuildAssistClient.getActivePanel() == null) { baCtrlDragging = false; return; }
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (!ctrl) { baCtrlDragging = false; return; }
        Slot slot = getSlotAt(click.x(), click.y());
        if (slot == null || slot.getStack().isEmpty()) return;
        if (!(slot.inventory instanceof PlayerInventory)) return;
        if (!baCtrlDraggedSlots.add(slot.id)) return;
        ItemStack stack = slot.getStack();
        ModMessaging.sendDeposit(Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount());
    }

    // End Ctrl+drag session on mouse release.
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onCtrlDragEnd(Click click, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == 0) {
            baCtrlDragging = false;
            baCtrlDraggedSlots.clear();
        }
    }

    @Inject(
        method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (BuildAssistClient.getActivePanel() == null) return;

        // Prevent cursor items from being dropped when clicking outside all slots —
        // deposit into storage only when the click is inside the storage panel.
        if (slot == null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
                if (!cursor.isEmpty()) {
                    double win = mc.getWindow().getWidth();
                    double h   = mc.getWindow().getHeight();
                    double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth()  / win;
                    double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / h;
                    StoragePanel panel = BuildAssistClient.getActivePanel();
                    if (panel != null && panel.isInsidePanel(mx, my)) {
                        ModMessaging.sendDeposit(
                            Registries.ITEM.getId(cursor.getItem()).toString(),
                            cursor.getCount()
                        );
                        ci.cancel();
                    }
                    return;
                }
            }
            ci.cancel();
            return;
        }

        // Only act on player-inventory slots (not container/chest slots).
        if (!(slot.inventory instanceof PlayerInventory)) return;

        // Ctrl+double-click: deposit all items of that type from player inventory.
        if (actionType == SlotActionType.PICKUP_ALL && !slot.getStack().isEmpty()) {
            long win = MinecraftClient.getInstance().getWindow().getHandle();
            boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
            if (ctrl) {
                String itemKey = Registries.ITEM.getId(slot.getStack().getItem()).toString();
                int total = countItemsInInventory(itemKey);
                if (total > 0) {
                    ModMessaging.sendDeposit(itemKey, total);
                }
                ci.cancel();
            }
            return;
        }

        // Ctrl+left-click: deposit the clicked slot's item into storage.
        if (button != 0 || actionType != SlotActionType.PICKUP) return;
        if (slot.getStack().isEmpty()) return;

        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (!ctrl) return;

        // Track in drag set so mouseDragged doesn't re-deposit the same slot.
        baCtrlDraggedSlots.add(slotId);

        ItemStack stack = slot.getStack();
        ModMessaging.sendDeposit(
            Registries.ITEM.getId(stack.getItem()).toString(),
            stack.getCount()
        );
        ci.cancel();
    }

    @Unique
    private static int countItemsInInventory(String itemKey) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;
        int total = 0;
        // Iterate main inventory slots (0-35: hotbar 0-8 + main 9-35)
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && Registries.ITEM.getId(s.getItem()).toString().equals(itemKey)) {
                total += s.getCount();
            }
        }
        ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
        if (!cursor.isEmpty() && Registries.ITEM.getId(cursor.getItem()).toString().equals(itemKey)) {
            total += cursor.getCount();
        }
        return total;
    }
}
