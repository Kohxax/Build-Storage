package dev.buildassist.mod.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class ItemCountRenderer {

    private ItemCountRenderer() {}

    public static void render(DrawContext context, long count, int x, int y) {
        if (count <= 0) return;
        String label = format(count);
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int textX = x + 16 - font.getWidth(label);
        int textY = y + 16 - font.fontHeight + 1;
        // Shadow then text for readability
        context.drawText(font, label, textX + 1, textY + 1, 0xFF000000, false);
        context.drawText(font, label, textX, textY, 0xFFFFFFFF, false);
    }

    public static String format(long count) {
        if (count >= 1_000_000) {
            long m = count / 100_000;
            return (m / 10) + (m % 10 == 0 ? "" : "." + (m % 10)) + "m";
        }
        if (count >= 1_000) {
            long k = count / 100;
            return (k / 10) + (k % 10 == 0 ? "" : "." + (k % 10)) + "k";
        }
        return String.valueOf(count);
    }
}
