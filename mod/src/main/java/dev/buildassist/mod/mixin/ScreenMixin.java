package dev.buildassist.mod.mixin;

import dev.buildassist.mod.client.BuildAssistClient;
import dev.buildassist.mod.client.screen.StoragePanel;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Element.class)
public interface ScreenMixin {

    @Inject(method = "charTyped(Lnet/minecraft/client/input/CharInput;)Z", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharInput charInput, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof InventoryScreen)) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.charTyped(charInput)) {
            cir.setReturnValue(true);
        }
    }

    // Intercept mouse release to handle drag-deposit onto the storage panel (issues 18/19)
    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object)this instanceof InventoryScreen)) return;
        StoragePanel panel = BuildAssistClient.getActivePanel();
        if (panel != null && panel.mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
}
