package dev.buildassist.mod.client.screen;

import dev.buildassist.mod.client.StorageCache;
import dev.buildassist.mod.client.config.BuildAssistConfig;
import dev.buildassist.mod.client.config.PanelSide;
import dev.buildassist.mod.client.render.GrayscaleRenderer;
import dev.buildassist.mod.client.render.ItemCountRenderer;
import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class StoragePanel {

    // Layout constants
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_WIDTH = COLS * SLOT_SIZE + 14;
    private static final int PANEL_HEIGHT = VISIBLE_ROWS * SLOT_SIZE + 48; // tabs + search bar

    private static final int TAB_HEIGHT = 14;
    private static final int SEARCH_HEIGHT = 14;

    // Vanilla GUI palette
    private static final int BG_COLOR       = 0xFFC6C6C6;  // light gray panel fill
    private static final int BORDER_DARK    = 0xFF373737;  // outer dark edge
    private static final int BEVEL_LIGHT    = 0xFFFFFFFF;  // top/left inner bevel
    private static final int BEVEL_SHADOW   = 0xFF555555;  // bottom/right inner bevel
    private static final int SLOT_BG        = 0xFF8B8B8B;  // slot interior
    private static final int SLOT_DARK      = 0xFF373737;  // slot top/left (sunken)
    private static final int SLOT_LIGHT     = 0xFFFFFFFF;  // slot bottom/right (sunken)
    private static final int TAB_ACTIVE     = 0xFFC6C6C6;  // active tab blends into panel
    private static final int TAB_INACTIVE   = 0xFF8B8B8B;  // inactive tab darker
    private static final int SEARCH_BG      = 0xFF3B3B3B;  // search field bg (dark)
    private static final int HOVER_COLOR    = 0x80FFFFFF;  // slot hover overlay

    private final StorageCache cache;
    private final BuildAssistConfig config;

    private int panelX;
    private int panelY;

    private int currentTabIndex = 0;
    private int scrollOffset = 0;

    private String searchQuery = "";
    private TextFieldWidget searchField;

    private List<StoragePanelHandler.SlotEntry> currentSlots = new ArrayList<>();
    private final List<RegistryKey<ItemGroup>> tabKeys = new ArrayList<>();

    private int hoveredSlot = -1;

    public StoragePanel(InventoryScreen inventoryScreen, BuildAssistConfig config) {
        this.cache = StorageCache.INSTANCE;
        this.config = config;
        calculatePosition(inventoryScreen);
        initTabs();
        buildSearchField();
        refreshSlots();
    }

    private void calculatePosition(InventoryScreen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int invX = (sw - 176) / 2 + config.getInventoryOffsetX();
        int invY = (sh - 166) / 2 + config.getInventoryOffsetY();

        PanelSide side = config.getPanelSide();
        int baseX = switch (side) {
            case LEFT  -> invX - PANEL_WIDTH - 4 + config.getPanelOffsetX();
            case RIGHT -> invX + 176 + 4 + config.getPanelOffsetX();
            case UP    -> invX + config.getPanelOffsetX();
            case DOWN  -> invX + config.getPanelOffsetX();
        };
        int baseY = switch (side) {
            case LEFT, RIGHT -> invY + config.getPanelOffsetY();
            case UP          -> invY - PANEL_HEIGHT - 4 + config.getPanelOffsetY();
            case DOWN        -> invY + 166 + 4 + config.getPanelOffsetY();
        };
        this.panelX = baseX;
        this.panelY = baseY;
    }

    private void initTabs() {
        tabKeys.clear();
        // Add vanilla item group keys in the creative tab order
        tabKeys.add(ItemGroups.BUILDING_BLOCKS);
        tabKeys.add(ItemGroups.COLORED_BLOCKS);
        tabKeys.add(ItemGroups.NATURAL);
        tabKeys.add(ItemGroups.FUNCTIONAL);
        tabKeys.add(ItemGroups.REDSTONE);
        tabKeys.add(ItemGroups.TOOLS);
        tabKeys.add(ItemGroups.COMBAT);
        tabKeys.add(ItemGroups.FOOD_AND_DRINK);
        tabKeys.add(ItemGroups.INGREDIENTS);
        tabKeys.add(ItemGroups.SPAWN_EGGS);
    }

    private void buildSearchField() {
        MinecraftClient client = MinecraftClient.getInstance();
        int fieldX = panelX + 5;
        int fieldY = panelY + TAB_HEIGHT + 3;
        searchField = new TextFieldWidget(client.textRenderer, fieldX, fieldY, PANEL_WIDTH - 10, SEARCH_HEIGHT, Text.literal(""));
        searchField.setMaxLength(64);
        searchField.setDrawsBackground(false);
        searchField.setText(searchQuery);
        searchField.setChangedListener(text -> {
            searchQuery = text;
            scrollOffset = 0;
            refreshSlots();
        });
    }

    private void refreshSlots() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || tabKeys.isEmpty()) {
            currentSlots = new ArrayList<>();
            return;
        }

        // Collect items from the current tab
        List<ItemStack> tabItems = new ArrayList<>();
        RegistryKey<ItemGroup> tabKey = tabKeys.get(currentTabIndex);
        var groups = client.world.getRegistryManager()
            .getOptional(net.minecraft.registry.RegistryKeys.ITEM_GROUP);
        if (groups.isPresent()) {
            var group = groups.get().get(tabKey);
            if (group != null) {
                tabItems.addAll(group.getDisplayStacks());
            }
        }

        StoragePanelHandler handler = new StoragePanelHandler(cache);
        currentSlots = handler.buildSlots(tabItems, searchQuery);
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int contentStartY = panelY + TAB_HEIGHT + SEARCH_HEIGHT + 6;

        // Raised panel background
        drawRaisedPanel(ctx, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Tab bar
        renderTabs(ctx, mouseX, mouseY);

        // Search field: sunken inset
        int sfX = panelX + 4;
        int sfY = panelY + TAB_HEIGHT + 2;
        int sfW = PANEL_WIDTH - 8;
        int sfH = SEARCH_HEIGHT + 2;
        drawSunkenSlot(ctx, sfX, sfY, sfW, sfH);
        ctx.fill(sfX + 1, sfY + 1, sfX + sfW - 1, sfY + sfH - 1, SEARCH_BG);
        searchField.render(ctx, mouseX, mouseY, delta);

        // Item grid
        hoveredSlot = -1;
        int firstSlot = scrollOffset * COLS;
        for (int i = 0; i < VISIBLE_ROWS * COLS; i++) {
            int slotIndex = firstSlot + i;
            int col = i % COLS;
            int row = i / COLS;
            int sx = panelX + 5 + col * SLOT_SIZE;
            int sy = contentStartY + row * SLOT_SIZE;

            // Always draw slot background
            drawSunkenSlot(ctx, sx - 1, sy - 1, SLOT_SIZE, SLOT_SIZE);

            if (slotIndex >= currentSlots.size()) continue;
            StoragePanelHandler.SlotEntry entry = currentSlots.get(slotIndex);

            // Hover highlight
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                ctx.fill(sx, sy, sx + 16, sy + 16, HOVER_COLOR);
                hoveredSlot = slotIndex;
            }

            GrayscaleRenderer.renderSlot(ctx, entry.displayStack, sx, sy, entry.isOwned());
            if (entry.isOwned()) {
                ItemCountRenderer.render(ctx, entry.count, sx, sy);
            }
        }

        // Tooltip for hovered slot
        if (hoveredSlot >= 0 && hoveredSlot < currentSlots.size()) {
            StoragePanelHandler.SlotEntry entry = currentSlots.get(hoveredSlot);
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(entry.displayStack.getName());
            if (entry.isOwned()) {
                tooltip.add(Text.literal("§7在庫: §e" + ItemCountRenderer.format(entry.count)));
            } else {
                tooltip.add(Text.literal("§8未所持"));
            }
            ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, mouseX, mouseY);
        }

        // Scrollbar
        renderScrollbar(ctx, contentStartY);
    }

    private void drawRaisedPanel(DrawContext ctx, int x, int y, int w, int h) {
        // Outer dark edge
        ctx.fill(x,         y,         x + w,     y + 1,     BORDER_DARK);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     BORDER_DARK);
        ctx.fill(x,         y,         x + 1,     y + h,     BORDER_DARK);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     BORDER_DARK);
        // Inner bevel
        ctx.fill(x + 1,     y + 1,     x + w - 1, y + 2,     BEVEL_LIGHT);
        ctx.fill(x + 1,     y + 1,     x + 2,     y + h - 1, BEVEL_LIGHT);
        ctx.fill(x + 1,     y + h - 2, x + w - 1, y + h - 1, BEVEL_SHADOW);
        ctx.fill(x + w - 2, y + 1,     x + w - 1, y + h - 1, BEVEL_SHADOW);
        // Fill
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, BG_COLOR);
    }

    private void drawSunkenSlot(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x,         y,         x + w,     y + h,     SLOT_BG);
        ctx.fill(x,         y,         x + w,     y + 1,     SLOT_DARK);
        ctx.fill(x,         y,         x + 1,     y + h,     SLOT_DARK);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     SLOT_LIGHT);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     SLOT_LIGHT);
    }

    private void renderTabs(DrawContext ctx, int mouseX, int mouseY) {
        int tabWidth = PANEL_WIDTH / tabKeys.size();
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        for (int i = 0; i < tabKeys.size(); i++) {
            int tx = panelX + i * tabWidth;
            boolean active = (i == currentTabIndex);
            int color = active ? TAB_ACTIVE : TAB_INACTIVE;
            // Tab background
            ctx.fill(tx, panelY, tx + tabWidth - 1, panelY + TAB_HEIGHT, color);
            // Top/left bevel for raised look
            ctx.fill(tx,             panelY,     tx + tabWidth - 1, panelY + 1,     active ? BEVEL_LIGHT : BORDER_DARK);
            ctx.fill(tx,             panelY,     tx + 1,            panelY + TAB_HEIGHT, active ? BEVEL_LIGHT : BORDER_DARK);
            // Number label
            String label = String.valueOf(i + 1);
            int labelColor = active ? 0xFF404040 : 0xFFDDDDDD;
            ctx.drawText(font, label, tx + tabWidth / 2 - font.getWidth(label) / 2, panelY + 3, labelColor, false);
        }
    }

    private void renderScrollbar(DrawContext ctx, int contentStartY) {
        int totalRows = (int) Math.ceil((double) currentSlots.size() / COLS);
        if (totalRows <= VISIBLE_ROWS) return;

        int barX = panelX + PANEL_WIDTH - 6;
        int barAreaHeight = VISIBLE_ROWS * SLOT_SIZE;
        int barHeight = Math.max(10, barAreaHeight * VISIBLE_ROWS / totalRows);
        int barY = contentStartY + (barAreaHeight - barHeight) * scrollOffset / Math.max(1, totalRows - VISIBLE_ROWS);

        ctx.fill(barX, contentStartY, barX + 4, contentStartY + barAreaHeight, 0xFF222222);
        ctx.fill(barX, barY, barX + 4, barY + barHeight, 0xFF888888);
    }

    public boolean mouseClicked(Click click, boolean consumed) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // If outside the panel, unfocus search field and let vanilla handle it
        if (mouseX < panelX || mouseX >= panelX + PANEL_WIDTH || mouseY < panelY || mouseY >= panelY + PANEL_HEIGHT) {
            searchField.setFocused(false);
            return false;
        }

        if (searchField.mouseClicked(click, consumed)) return true;

        // Tab click
        int tabWidth = PANEL_WIDTH / tabKeys.size();
        if (mouseY >= panelY && mouseY < panelY + TAB_HEIGHT) {
            int idx = (int) ((mouseX - panelX) / tabWidth);
            if (idx >= 0 && idx < tabKeys.size()) {
                currentTabIndex = idx;
                scrollOffset = 0;
                refreshSlots();
                return true;
            }
        }

        // Slot click
        if (hoveredSlot >= 0 && hoveredSlot < currentSlots.size()) {
            StoragePanelHandler.SlotEntry entry = currentSlots.get(hoveredSlot);
            if (entry.isOwned()) {
                long win = MinecraftClient.getInstance().getWindow().getHandle();
                boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                             || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                int amount = switch (button) {
                    case 0 -> entry.displayStack.getItem().getMaxCount();
                    case 1 -> Math.max(1, entry.displayStack.getItem().getMaxCount() / 2);
                    default -> 1;
                };
                ModMessaging.sendWithdraw(entry.itemKey, (int) Math.min(amount, entry.count), shift);
            }
            return true;
        }

        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentStartY = panelY + TAB_HEIGHT + SEARCH_HEIGHT + 6;
        int contentEndY = contentStartY + VISIBLE_ROWS * SLOT_SIZE;
        if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH || mouseY < contentStartY || mouseY > contentEndY) {
            return false;
        }
        int totalRows = (int) Math.ceil((double) currentSlots.size() / COLS);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(verticalAmount), totalRows - VISIBLE_ROWS));
        return true;
    }

    public boolean charTyped(CharInput charInput) {
        return searchField.charTyped(charInput);
    }

    public boolean keyPressed(KeyInput keyInput) {
        return searchField.keyPressed(keyInput);
    }

    public void onStorageUpdate() {
        refreshSlots();
    }
}
