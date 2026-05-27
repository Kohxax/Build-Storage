package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into Screen.charTyped so the mixin fires for InventoryScreen instances.
 * Element-interface mixins are skipped when Screen (a class) overrides the method,
 * so targeting Screen directly is the reliable approach.
 */
@Mixin(Screen.class)
public abstract class ScreenCharMixin {

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharInput charInput, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof InventoryScreen)) return;
        if (StoragePanel.isHandlingCharTyped()) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel == null) return;
        StoragePanel.setHandlingCharTyped(true);
        try {
            if (panel.charTyped(charInput)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        } finally {
            StoragePanel.setHandlingCharTyped(false);
        }
    }
}
