package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts Keyboard.onChar (the GLFW char callback) to route character input
 * to the storage panel before the screen receives it.  This mirrors how Fabric's
 * own KeyboardHandlerMixin hooks onKey for its key-press events.
 */
@Mixin(Keyboard.class)
public class ScreenCharMixin {

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(long window, CharInput charInput, CallbackInfo ci) {
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel == null) return;
        if (panel.charTyped(charInput)) {
            ci.cancel();
        }
    }
}
