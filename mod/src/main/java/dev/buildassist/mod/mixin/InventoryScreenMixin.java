package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.render.ItemCountRenderer;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BuildAssistClient.onInventoryOpen((InventoryScreen)(Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
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
}
