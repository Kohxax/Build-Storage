package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "removed()V", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if ((Object)this instanceof InventoryScreen) {
            BuildAssistClient.onInventoryClose();
        }
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/gui/Click;Z)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean consumed, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof InventoryScreen)) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseClicked(click, consumed)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof InventoryScreen)) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyInput;)Z", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof InventoryScreen)) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.keyPressed(keyInput)) {
            cir.setReturnValue(true);
        }
    }
}
