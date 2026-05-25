package dev.buildassist.mod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.item.ItemStack;

public class GrayscaleRenderer {

    private GrayscaleRenderer() {}

    // Renders an item in grayscale by drawing a dark overlay over the slot
    public static void renderGrayscaleOverlay(DrawContext context, int x, int y) {
        // Draw a semi-transparent dark overlay to simulate grayscale/disabled appearance
        context.fill(x, y, x + 16, y + 16, 0xAA303030);
    }

    // Draw the item normally or in grayscale depending on owned flag
    public static void renderSlot(DrawContext context, ItemStack stack, int x, int y, boolean owned) {
        context.drawItem(stack, x, y);
        if (!owned) {
            renderGrayscaleOverlay(context, x, y);
        }
    }
}
