package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        BuildAssistClient.onInventoryOpen(self);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null) {
            panel.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        BuildAssistClient.onInventoryClose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseClicked(mouseX, mouseY, button)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(char chr, int modifiers, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.charTyped(chr, modifiers)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.keyPressed(keyCode, scanCode, modifiers)) {
            ci.cancel();
        }
    }
}
