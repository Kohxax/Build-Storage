package dev.buildassist.mod.client.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private static final int BUTTON_W = 100;
    private static final int BUTTON_H = 20;
    private static final int SPACING = 6;

    private final Screen parent;
    private final BuildAssistConfig config;

    // Drag state for panel position adjustment
    private boolean draggingPanel = false;
    private boolean draggingInventory = false;
    private double dragStartX, dragStartY;
    private int dragBaseX, dragBaseY;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("buildassist.screen.config.title"));
        this.parent = parent;
        this.config = BuildAssistConfig.get();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        // Panel side buttons
        int y = cy - 80;
        addDrawableChild(ButtonWidget.builder(sideLabel(PanelSide.LEFT), b -> setSide(PanelSide.LEFT))
            .dimensions(cx - BUTTON_W - SPACING / 2, y, BUTTON_W, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(sideLabel(PanelSide.RIGHT), b -> setSide(PanelSide.RIGHT))
            .dimensions(cx + SPACING / 2, y, BUTTON_W, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(sideLabel(PanelSide.UP), b -> setSide(PanelSide.UP))
            .dimensions(cx - BUTTON_W / 2, y + BUTTON_H + SPACING, BUTTON_W, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(sideLabel(PanelSide.DOWN), b -> setSide(PanelSide.DOWN))
            .dimensions(cx - BUTTON_W / 2, y + (BUTTON_H + SPACING) * 2, BUTTON_W, BUTTON_H).build());

        // Offset nudge buttons for panel
        int nudgeY = cy + 20;
        addDrawableChild(ButtonWidget.builder(Text.literal("Panel ←"), b -> nudgePanel(-4, 0))
            .dimensions(cx - 160, nudgeY, 70, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Panel →"), b -> nudgePanel(4, 0))
            .dimensions(cx - 84, nudgeY, 70, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Panel ↑"), b -> nudgePanel(0, -4))
            .dimensions(cx - 160, nudgeY + BUTTON_H + SPACING, 70, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Panel ↓"), b -> nudgePanel(0, 4))
            .dimensions(cx - 84, nudgeY + BUTTON_H + SPACING, 70, BUTTON_H).build());

        // Offset nudge buttons for inventory
        addDrawableChild(ButtonWidget.builder(Text.literal("Inv ←"), b -> nudgeInventory(-4, 0))
            .dimensions(cx + 14, nudgeY, 70, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Inv →"), b -> nudgeInventory(4, 0))
            .dimensions(cx + 90, nudgeY, 70, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Inv ↑"), b -> nudgeInventory(0, -4))
            .dimensions(cx + 14, nudgeY + BUTTON_H + SPACING, 70, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Inv ↓"), b -> nudgeInventory(0, 4))
            .dimensions(cx + 90, nudgeY + BUTTON_H + SPACING, 70, BUTTON_H).build());

        // Reset & Done
        addDrawableChild(ButtonWidget.builder(Text.literal("リセット"), b -> resetOffsets())
            .dimensions(cx - 50, cy + 90, BUTTON_W, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("完了"), b -> close())
            .dimensions(cx - 50, cy + 90 + BUTTON_H + SPACING, BUTTON_W, BUTTON_H).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);

        // Current settings info
        String info = String.format("Side: %s  Panel offset: (%d, %d)  Inv offset: (%d, %d)",
            config.getPanelSide(),
            config.getPanelOffsetX(), config.getPanelOffsetY(),
            config.getInventoryOffsetX(), config.getInventoryOffsetY());
        ctx.drawCenteredTextWithShadow(textRenderer, info, width / 2, height / 2 - 100, 0xAAAAAA);

        ctx.drawCenteredTextWithShadow(textRenderer, "← パネル位置 →     ← インベントリ位置 →", width / 2, height / 2 + 14, 0xAAAAAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        config.save();
        client.setScreen(parent);
    }

    private Text sideLabel(PanelSide side) {
        String arrow = switch (side) {
            case LEFT -> "◀ 左";
            case RIGHT -> "右 ▶";
            case UP -> "▲ 上";
            case DOWN -> "▼ 下";
        };
        boolean active = config.getPanelSide() == side;
        return Text.literal((active ? "§e" : "§7") + arrow);
    }

    private void setSide(PanelSide side) {
        config.setPanelSide(side);
        config.save();
        clearAndInit();
    }

    private void nudgePanel(int dx, int dy) {
        config.setPanelOffsetX(config.getPanelOffsetX() + dx);
        config.setPanelOffsetY(config.getPanelOffsetY() + dy);
        config.save();
    }

    private void nudgeInventory(int dx, int dy) {
        config.setInventoryOffsetX(config.getInventoryOffsetX() + dx);
        config.setInventoryOffsetY(config.getInventoryOffsetY() + dy);
        config.save();
    }

    private void resetOffsets() {
        config.setPanelOffsetX(0);
        config.setPanelOffsetY(0);
        config.setInventoryOffsetX(0);
        config.setInventoryOffsetY(0);
        config.save();
    }

    @Override
    public boolean shouldPause() { return false; }
}
