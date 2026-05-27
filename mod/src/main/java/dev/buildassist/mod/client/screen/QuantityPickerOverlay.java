package dev.buildassist.mod.client.screen;

import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class QuantityPickerOverlay {

    private static final int W         = 130;
    private static final int H         = 80;
    private static final int BTN_W     = 56;
    private static final int BTN_H     = 14;
    private static final int FIELD_H   = 14;
    private static final int SMALL_BTN = 18;

    // y offsets within overlay
    private static final int ROW_TITLE   = 5;
    private static final int ROW_MODE    = 18;
    private static final int ROW_INPUT   = 34;
    private static final int ROW_INFO    = 52;
    private static final int ROW_CONFIRM = H - BTN_H - 4; // 62

    private enum Mode { COUNT, STACK }
    private Mode mode = Mode.COUNT;

    private StoragePanelHandler.SlotEntry entry;
    private TextFieldWidget amountField;
    private boolean open = false;

    private int ox, oy;

    public boolean isOpen() { return open; }

    public void open(int panelX, int panelY, int panelW, StoragePanelHandler.SlotEntry entry) {
        this.entry = entry;
        this.mode  = Mode.COUNT;
        MinecraftClient mc = MinecraftClient.getInstance();
        this.ox = panelX + (panelW - W) / 2;
        this.oy = panelY + 14;

        amountField = new TextFieldWidget(mc.textRenderer,
            ox + 26, oy + ROW_INPUT, 78, FIELD_H, Text.literal(""));
        amountField.setMaxLength(8);
        amountField.setFocused(true);
        amountField.setText("1");
        amountField.setCursorToEnd(false);

        this.open = true;
    }

    public void close() {
        open = false;
        entry = null;
        amountField = null;
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!open) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        // Dim overlay
        ctx.fill(ox - 2000, oy - 2000, ox + 2000 + W, oy + 2000 + H, 0x80000000);

        // Panel background + border
        ctx.fill(ox, oy, ox + W, oy + H, 0xFF2D2D2D);
        ctx.fill(ox,         oy,         ox + W,     oy + 1,     0xFF888888);
        ctx.fill(ox,         oy + H - 1, ox + W,     oy + H,     0xFF888888);
        ctx.fill(ox,         oy,         ox + 1,     oy + H,     0xFF888888);
        ctx.fill(ox + W - 1, oy,         ox + W,     oy + H,     0xFF888888);

        // Title + stock
        String title = entry.displayStack.getName().getString();
        String stock = "在庫: " + entry.count;
        ctx.drawText(mc.textRenderer, title, ox + 6, oy + ROW_TITLE, 0xFFFFFF, false);
        ctx.drawText(mc.textRenderer, stock,
            ox + W - 6 - mc.textRenderer.getWidth(stock), oy + ROW_TITLE, 0xAAAAAA, false);

        // Mode toggle
        boolean countActive = mode == Mode.COUNT;
        renderButton(ctx, mouseX, mouseY, ox + 6,  oy + ROW_MODE, BTN_W, BTN_H, "個数",    countActive);
        renderButton(ctx, mouseX, mouseY, ox + 68, oy + ROW_MODE, BTN_W, BTN_H, "スタック", !countActive);

        // - field + row
        renderButton(ctx, mouseX, mouseY, ox + 4,   oy + ROW_INPUT, SMALL_BTN, FIELD_H, "－", false);
        amountField.setPosition(ox + 26, oy + ROW_INPUT);
        amountField.setWidth(78);
        amountField.render(ctx, mouseX, mouseY, delta);
        renderButton(ctx, mouseX, mouseY, ox + 108, oy + ROW_INPUT, SMALL_BTN, FIELD_H, "＋", false);

        // Info line (stack preview or inventory-full warning)
        int actual = resolveAmount();
        if (isInventoryFull()) {
            ctx.drawText(mc.textRenderer, "インベントリが満杯", ox + 6, oy + ROW_INFO, 0xFF4444, false);
        } else if (mode == Mode.STACK) {
            String preview = actual >= 0 ? "= " + actual + "個" : "無効な入力";
            ctx.drawText(mc.textRenderer, preview, ox + 6, oy + ROW_INFO,
                actual >= 0 ? 0x88FF88 : 0xFF6666, false);
        }

        // Confirm / Cancel
        boolean valid = actual > 0;
        renderButton(ctx, mouseX, mouseY, ox + 4,  oy + ROW_CONFIRM, BTN_W, BTN_H, "確定",      false, !valid);
        renderButton(ctx, mouseX, mouseY, ox + 70, oy + ROW_CONFIRM, BTN_W, BTN_H, "キャンセル", false, false);
    }

    private void renderButton(DrawContext ctx, int mx, int my,
                               int bx, int by, int bw, int bh,
                               String label, boolean active) {
        renderButton(ctx, mx, my, bx, by, bw, bh, label, active, false);
    }

    private void renderButton(DrawContext ctx, int mx, int my,
                               int bx, int by, int bw, int bh,
                               String label, boolean active, boolean disabled) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean hovered = !disabled && mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        int bg     = disabled ? 0xFF3A3A3A : active ? 0xFF5A7A5A : hovered ? 0xFF5A5A7A : 0xFF444444;
        int border = disabled ? 0xFF555555 : active ? 0xFF88CC88 : 0xFF666688;
        int text   = disabled ? 0xFF888888 : 0xFFEEEEEE;

        ctx.fill(bx, by, bx + bw, by + bh, bg);
        ctx.fill(bx,          by,          bx + bw, by + 1,     border);
        ctx.fill(bx,          by + bh - 1, bx + bw, by + bh,    border);
        ctx.fill(bx,          by,          bx + 1,  by + bh,    border);
        ctx.fill(bx + bw - 1, by,          bx + bw, by + bh,    border);

        int lx = bx + (bw - mc.textRenderer.getWidth(label)) / 2;
        int ly = by + (bh - 9) / 2;
        ctx.drawText(mc.textRenderer, label, lx, ly, text, false);
    }

    // ─── Input ────────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        if (!open) return false;
        int mxi = (int) mx, myi = (int) my;

        if (mxi < ox || mxi >= ox + W || myi < oy || myi >= oy + H) {
            close();
            return true;
        }

        // Mode toggle row
        if (myi >= oy + ROW_MODE && myi < oy + ROW_MODE + BTN_H) {
            if (mxi >= ox + 6  && mxi < ox + 6  + BTN_W) { mode = Mode.COUNT; return true; }
            if (mxi >= ox + 68 && mxi < ox + 68 + BTN_W) { mode = Mode.STACK; return true; }
        }

        // - / field / + row
        if (myi >= oy + ROW_INPUT && myi < oy + ROW_INPUT + FIELD_H) {
            if (mxi >= ox + 4 && mxi < ox + 4 + SMALL_BTN) {
                adjustValue(-1);
                return true;
            }
            if (mxi >= ox + 108 && mxi < ox + 108 + SMALL_BTN) {
                adjustValue(+1);
                return true;
            }
            if (amountField != null) {
                amountField.setFocused(mxi >= ox + 26 && mxi < ox + 104);
            }
        }

        // Confirm / Cancel
        if (myi >= oy + ROW_CONFIRM && myi < oy + ROW_CONFIRM + BTN_H) {
            if (mxi >= ox + 4 && mxi < ox + 4 + BTN_W) {
                int amount = resolveAmount();
                if (amount > 0) {
                    int maxStack = entry.displayStack.getItem().getMaxCount();
                    // Use shift=false (cursor) for single-stack amounts so the item
                    // lands on the cursor like a normal left-click.  Use shift=true
                    // (inventory) only when the total exceeds one stack.
                    boolean shift = amount > maxStack;
                    ModMessaging.sendWithdraw(entry.itemKey, amount, shift);
                    close();
                }
            } else if (mxi >= ox + 70 && mxi < ox + 70 + BTN_W) {
                close();
            }
            return true;
        }

        return true;
    }

    public boolean charTyped(CharInput charInput) {
        if (!open || amountField == null) return false;
        if (amountField.charTyped(charInput)) return true;
        if (charInput.isValidChar()) {
            String s = charInput.asString();
            if (s.matches("[0-9]")) {
                int cursor = amountField.getCursor();
                String current = amountField.getText();
                amountField.setText(current.substring(0, cursor) + s + current.substring(cursor));
                amountField.setCursorToEnd(false);
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (!open) return false;
        int key = keyInput.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            int amount = resolveAmount();
            if (amount > 0) {
                int maxStack = entry.displayStack.getItem().getMaxCount();
                boolean shift = amount > maxStack;
                ModMessaging.sendWithdraw(entry.itemKey, amount, shift);
                close();
            }
            return true;
        }
        if (amountField != null) {
            amountField.keyPressed(keyInput);
        }
        return true;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void adjustValue(int delta) {
        if (amountField == null) return;
        int current = 0;
        try { current = Integer.parseInt(amountField.getText().trim()); } catch (NumberFormatException ignored) {}
        int maxStack = entry != null ? entry.displayStack.getItem().getMaxCount() : 1;
        int step = (mode == Mode.STACK) ? 1 : 1;
        int newVal = Math.max(1, current + delta * step);
        amountField.setText(String.valueOf(newVal));
        amountField.setCursorToEnd(false);
    }

    /** Returns resolved item count, or -1 if invalid. Clamped to [1, stock]. */
    private int resolveAmount() {
        if (amountField == null || entry == null) return -1;
        String text = amountField.getText().trim();
        if (text.isEmpty()) return -1;
        try {
            int value = Integer.parseInt(text);
            if (value <= 0) return -1;
            int maxStack = entry.displayStack.getItem().getMaxCount();
            long actual = mode == Mode.STACK ? (long) value * maxStack : value;
            return (int) Math.min(actual, entry.count);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isInventoryFull() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        return mc.player.getInventory().getEmptySlot() < 0;
    }
}
