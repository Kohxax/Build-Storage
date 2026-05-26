package dev.buildassist.mod.client.screen;

import dev.buildassist.mod.client.StorageCache;
import dev.buildassist.mod.client.config.BuildAssistConfig;
import dev.buildassist.mod.client.config.PanelSide;
import dev.buildassist.mod.client.render.GrayscaleRenderer;
import dev.buildassist.mod.client.render.ItemCountRenderer;
import dev.buildassist.mod.network.ModMessaging;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class StoragePanel {

    // Vanilla creative inventory exact dimensions
    private static final int PANEL_WIDTH  = 195;
    private static final int PANEL_HEIGHT = 136;
    private static final int COLS         = 9;
    private static final int VISIBLE_ROWS = 5;
    private static final int SLOT_SIZE    = 18;
    // Slot grid offset within the background texture (matches vanilla creative)
    private static final int GRID_X = 9;
    private static final int GRID_Y = 18;
    // Tab button dimensions (from vanilla bytecode)
    private static final int TAB_W        = 26;
    private static final int TAB_H        = 32;
    private static final int TAB_COL_STEP = 27;
    private static final int TAB_Y_OFFSET = 28; // tabs appear 28px above panel top
    // Scrollbar position within panel (from vanilla bytecode: x+175, area y+18 to y+130)
    private static final int SCROLL_X        = 175;
    private static final int SCROLL_AREA_TOP = 18;
    private static final int SCROLL_AREA_H   = 94; // 112 - 18
    private static final int SCROLL_H        = 15;
    private static final int SCROLL_W        = 12;

    // Vanilla creative GUI atlas sprites
    private static final Identifier SCROLLER =
        Identifier.ofVanilla("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED =
        Identifier.ofVanilla("container/creative_inventory/scroller_disabled");
    private static final Identifier[] TAB_TOP_SELECTED = {
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_1"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_2"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_3"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_4"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_5"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_6"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_selected_7"),
    };
    private static final Identifier[] TAB_TOP_UNSELECTED = {
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_1"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_2"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_3"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_4"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_5"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_6"),
        Identifier.ofVanilla("container/creative_inventory/tab_top_unselected_7"),
    };
    private static final Identifier[] TAB_BOTTOM_SELECTED = {
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_1"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_2"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_3"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_4"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_5"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_6"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_selected_7"),
    };
    private static final Identifier[] TAB_BOTTOM_UNSELECTED = {
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_1"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_2"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_3"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_4"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_5"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_6"),
        Identifier.ofVanilla("container/creative_inventory/tab_bottom_unselected_7"),
    };

    private static final int HOVER_COLOR = 0x80FFFFFF;

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
            case LEFT, RIGHT -> invY + TAB_Y_OFFSET + config.getPanelOffsetY();
            case UP          -> invY - PANEL_HEIGHT - TAB_Y_OFFSET - 4 + config.getPanelOffsetY();
            case DOWN        -> invY + 166 + TAB_Y_OFFSET + 4 + config.getPanelOffsetY();
        };
        this.panelX = baseX;
        this.panelY = baseY;
    }

    private void initTabs() {
        tabKeys.clear();
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
        // Position matches vanilla creative search box overlay within the panel header
        searchField = new TextFieldWidget(client.textRenderer,
            panelX + 82, panelY + 6, 79, 9, Text.literal(""));
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
        List<ItemStack> tabItems = new ArrayList<>();
        RegistryKey<ItemGroup> tabKey = tabKeys.get(currentTabIndex);
        var groups = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
        if (groups.isPresent()) {
            var group = groups.get().get(tabKey);
            if (group != null) {
                tabItems.addAll(group.getDisplayStacks());
            }
        }
        StoragePanelHandler handler = new StoragePanelHandler(cache);
        currentSlots = handler.buildSlots(tabItems, searchQuery);
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Unselected tabs drawn first (behind panel edge)
        renderAllTabs(ctx, mouseX, mouseY, false);

        // Background: exact vanilla creative tab_items texture (195×136, stored in 256×256)
        Identifier bgTexture = getSelectedGroupTexture();
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, bgTexture,
            panelX, panelY, 0f, 0f, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);

        // Search field overlaid on the panel header area
        searchField.render(ctx, mouseX, mouseY, delta);

        // Scrollbar
        renderScrollbar(ctx);

        // Selected tab drawn on top (overlaps panel border)
        renderAllTabs(ctx, mouseX, mouseY, true);

        // Item grid
        hoveredSlot = -1;
        int firstSlot = scrollOffset * COLS;
        for (int i = 0; i < VISIBLE_ROWS * COLS; i++) {
            int slotIndex = firstSlot + i;
            if (slotIndex >= currentSlots.size()) break;
            int col = i % COLS;
            int row = i / COLS;
            int sx = panelX + GRID_X + col * SLOT_SIZE;
            int sy = panelY + GRID_Y + row * SLOT_SIZE;

            StoragePanelHandler.SlotEntry entry = currentSlots.get(slotIndex);

            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                ctx.fill(sx, sy, sx + 16, sy + 16, HOVER_COLOR);
                hoveredSlot = slotIndex;
            }

            GrayscaleRenderer.renderSlot(ctx, entry.displayStack, sx, sy, entry.isOwned());
            if (entry.isOwned()) {
                ItemCountRenderer.render(ctx, entry.count, sx, sy);
            }
        }

        // Tooltip
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
    }

    private Identifier getSelectedGroupTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return Identifier.ofVanilla("container/creative_inventory/tab_items");
        var groups = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
        if (groups.isPresent()) {
            var group = groups.get().get(tabKeys.get(currentTabIndex));
            if (group != null) return group.getTexture();
        }
        return Identifier.ofVanilla("container/creative_inventory/tab_items");
    }

    private void renderAllTabs(DrawContext ctx, int mouseX, int mouseY, boolean selectedOnly) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        var groupRegistry = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
        if (groupRegistry.isEmpty()) return;

        for (int i = 0; i < tabKeys.size(); i++) {
            var group = groupRegistry.get().get(tabKeys.get(i));
            if (group == null) continue;
            boolean active = (i == currentTabIndex);
            if (active != selectedOnly) continue;
            renderTabIcon(ctx, group, active);
        }
    }

    private void renderTabIcon(DrawContext ctx, ItemGroup group, boolean active) {
        boolean isTop = group.getRow() == ItemGroup.Row.TOP;
        int col = group.getColumn();
        int tabX = panelX + col * TAB_COL_STEP;
        int tabY = isTop ? (panelY - TAB_Y_OFFSET) : (panelY + PANEL_HEIGHT - 4);

        Identifier[] sprites = isTop
            ? (active ? TAB_TOP_SELECTED    : TAB_TOP_UNSELECTED)
            : (active ? TAB_BOTTOM_SELECTED : TAB_BOTTOM_UNSELECTED);
        Identifier sprite = sprites[MathHelper.clamp(col, 0, sprites.length - 1)];

        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, tabX, tabY, TAB_W, TAB_H);

        // Item icon centered on the tab button (vanilla formula from bytecode)
        int iconX = tabX + 5;
        int iconY = tabY + 8 + (isTop ? 1 : -1);
        ctx.drawItem(group.getIcon(), iconX, iconY);
    }

    private void renderScrollbar(DrawContext ctx) {
        int totalRows = (int) Math.ceil((double) currentSlots.size() / COLS);
        boolean canScroll = totalRows > VISIBLE_ROWS;

        Identifier scrollSprite = canScroll ? SCROLLER : SCROLLER_DISABLED;
        int scrollX = panelX + SCROLL_X;
        float scrollPos = canScroll
            ? (float) scrollOffset / (totalRows - VISIBLE_ROWS)
            : 0f;
        int scrollY = panelY + SCROLL_AREA_TOP
            + MathHelper.floor((SCROLL_AREA_H - SCROLL_H) * scrollPos);

        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, scrollSprite, scrollX, scrollY, SCROLL_W, SCROLL_H);
    }

    // ─── Input ────────────────────────────────────────────────────────────────

    public boolean mouseClicked(Click click, boolean consumed) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        // Check if click is in a tab button area (above or below panel)
        int tabIdx = getTabAt(mouseX, mouseY);
        if (tabIdx >= 0) {
            currentTabIndex = tabIdx;
            scrollOffset = 0;
            refreshSlots();
            return true;
        }

        // Outside main panel: unfocus search and let vanilla handle
        if (mouseX < panelX || mouseX >= panelX + PANEL_WIDTH
                || mouseY < panelY || mouseY >= panelY + PANEL_HEIGHT) {
            searchField.setFocused(false);
            return false;
        }

        if (searchField.mouseClicked(click, consumed)) return true;

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

    private int getTabAt(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return -1;
        var groupRegistry = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
        if (groupRegistry.isEmpty()) return -1;

        for (int i = 0; i < tabKeys.size(); i++) {
            var group = groupRegistry.get().get(tabKeys.get(i));
            if (group == null) continue;
            boolean isTop = group.getRow() == ItemGroup.Row.TOP;
            int tabX = panelX + group.getColumn() * TAB_COL_STEP;
            int tabY = isTop ? (panelY - TAB_Y_OFFSET) : (panelY + PANEL_HEIGHT - 4);
            if (mouseX >= tabX && mouseX < tabX + TAB_W && mouseY >= tabY && mouseY < tabY + TAB_H) {
                return i;
            }
        }
        return -1;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int gridTop = panelY + GRID_Y;
        int gridBottom = gridTop + VISIBLE_ROWS * SLOT_SIZE;
        if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH || mouseY < gridTop || mouseY > gridBottom) {
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
