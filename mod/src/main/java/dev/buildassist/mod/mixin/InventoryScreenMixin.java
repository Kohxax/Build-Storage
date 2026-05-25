package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        BuildAssistClient.onInventoryOpen((InventoryScreen) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null) {
            panel.render(context, mouseX, mouseY, delta);
        }
    }

    @Inject(method = "removed()V", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        BuildAssistClient.onInventoryClose();
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                  CallbackInfoReturnable<Boolean> cir) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped(CI)Z", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.charTyped(chr, modifiers)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.keyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }
}
