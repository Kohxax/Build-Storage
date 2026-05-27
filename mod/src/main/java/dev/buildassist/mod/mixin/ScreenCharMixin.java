package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into Element.charTyped (method_25400), which is the only class where Loom
 * successfully remaps the method name.  The InventoryScreen instanceof check was removed
 * because in MC 1.21.11 charTyped may be dispatched to the *focused element* rather than
 * the screen itself, so `this` is never InventoryScreen.  Instead we guard on whether the
 * storage panel is active, and use a re-entrancy flag to prevent infinite recursion when
 * panel.charTyped() internally calls the text-field's own charTyped().
 */
@Mixin(net.minecraft.client.gui.Element.class)
public interface ScreenCharMixin {

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharInput charInput, CallbackInfoReturnable<Boolean> cir) {
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
