package dev.buildassist.mod.client.screen;

import dev.buildassist.mod.client.StorageCache;
import dev.buildassist.mod.client.StorageEntry;
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
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    // Clips off the hotbar rows at the bottom of the creative texture (clip at exact item grid bottom)
    private static final int PANEL_CLIP_HEIGHT = GRID_Y + VISIBLE_ROWS * SLOT_SIZE; // 108
    // Tab button dimensions (from vanilla bytecode)
    private static final int TAB_W        = 26;
    private static final int TAB_H        = 32;
    private static final int TAB_COL_STEP = 27;
    private static final int TAB_Y_OFFSET = 28;
    // Scrollbar position within panel (from vanilla bytecode: x+175, area y+18 to y+112)
    private static final int SCROLL_X        = 175;
    private static final int SCROLL_AREA_TOP = 18;
    private static final int SCROLL_AREA_H   = 94;
    private static final int SCROLL_H        = 15;
    private static final int SCROLL_W        = 12;

    // Sentinel keys for custom tabs (not real ItemGroups)
    private static final RegistryKey<ItemGroup> UNCATEGORIZED_KEY =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("buildassist", "uncategorized"));
    private static final RegistryKey<ItemGroup> SEARCH_KEY =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("buildassist", "search"));

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

    // Panel drag state
    private boolean isDragging = false;
    private int dragLastX, dragLastY;
    private boolean positionDirty = false;

    // Scrollbar drag state
    private boolean isScrollbarDragging = false;

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
        tabKeys.add(UNCATEGORIZED_KEY); // index 9, bottom col 2
        tabKeys.add(SEARCH_KEY);        // index 10, bottom col 3 — search across all stored items
    }

    private void buildSearchField() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (searchField != null) {
            // Panel repositioned — just move the existing widget
            searchField.setPosition(panelX + 82, panelY + 6);
            return;
        }
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
        searchField.setFocused(true);
    }

    private void refreshSlots() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || tabKeys.isEmpty()) {
            currentSlots = new ArrayList<>();
            return;
        }

        RegistryKey<ItemGroup> tabKey = tabKeys.get(currentTabIndex);

        if (tabKey.equals(SEARCH_KEY)) {
            currentSlots = buildSearchSlots();
            return;
        }

        if (tabKey.equals(UNCATEGORIZED_KEY)) {
            currentSlots = buildUncategorizedSlots(client);
            return;
        }

        List<ItemStack> tabItems = new ArrayList<>();
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

    private List<StoragePanelHandler.SlotEntry> buildUncategorizedSlots(MinecraftClient client) {
        var registryOpt = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);

        // Collect all item IDs that appear in any named tab
        Set<String> categorized = new HashSet<>();
        if (registryOpt.isPresent()) {
            for (RegistryKey<ItemGroup> key : tabKeys) {
                if (key.equals(UNCATEGORIZED_KEY)) continue;
                var group = registryOpt.get().get(key);
                if (group == null) continue;
                for (ItemStack stack : group.getDisplayStacks()) {
                    categorized.add(Registries.ITEM.getId(stack.getItem()).toString());
                }
            }
        }

        String lowerSearch = searchQuery.toLowerCase();
        List<StoragePanelHandler.SlotEntry> result = new ArrayList<>();

        for (StorageEntry entry : cache.getAll()) {
            String itemKey = entry.getItemKey();
            if (categorized.contains(itemKey)) continue;

            net.minecraft.item.Item item = Registries.ITEM.get(Identifier.of(itemKey));
            if (item == Items.AIR) continue;

            ItemStack stack = new ItemStack(item);
            String displayName = stack.getName().getString().toLowerCase();
            if (!lowerSearch.isEmpty() && !displayName.contains(lowerSearch) && !itemKey.contains(lowerSearch)) {
                continue;
            }

            result.add(new StoragePanelHandler.SlotEntry(stack, itemKey, entry.getCount()));
        }
        return result;
    }

    private List<StoragePanelHandler.SlotEntry> buildSearchSlots() {
        String lowerSearch = searchQuery.toLowerCase();
        List<StoragePanelHandler.SlotEntry> result = new ArrayList<>();
        for (StorageEntry entry : cache.getAll()) {
            String itemKey = entry.getItemKey();
            net.minecraft.item.Item item = Registries.ITEM.get(Identifier.of(itemKey));
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item);
            if (!lowerSearch.isEmpty()) {
                String displayName = stack.getName().getString().toLowerCase();
                if (!displayName.contains(lowerSearch) && !itemKey.contains(lowerSearch)) continue;
            }
            result.add(new StoragePanelHandler.SlotEntry(stack, itemKey, entry.getCount()));
        }
        return result;
    }

    // ─── Rendering ────────────────────────────────────────────────────────────

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean leftDown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        // Panel drag
        if (isDragging) {
            if (!leftDown) {
                isDragging = false;
                if (positionDirty) {
                    config.save();
                    positionDirty = false;
                }
            } else {
                int dx = mouseX - dragLastX;
                int dy = mouseY - dragLastY;
                if (dx != 0 || dy != 0) {
                    panelX += dx;
                    panelY += dy;
                    config.setPanelOffsetX(config.getPanelOffsetX() + dx);
                    config.setPanelOffsetY(config.getPanelOffsetY() + dy);
                    positionDirty = true;
                    dragLastX = mouseX;
                    dragLastY = mouseY;
                    buildSearchField(); // updates position only (field already exists)
                }
            }
        }

        // Scrollbar drag
        if (isScrollbarDragging) {
            if (!leftDown) {
                isScrollbarDragging = false;
            } else {
                int totalRows = (int) Math.ceil((double) currentSlots.size() / COLS);
                int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
                if (maxScroll > 0) {
                    int thumbRange = SCROLL_AREA_H - SCROLL_H;
                    float ratio = (float)(mouseY - panelY - SCROLL_AREA_TOP - SCROLL_H / 2) / thumbRange;
                    scrollOffset = MathHelper.clamp((int)(ratio * maxScroll), 0, maxScroll);
                }
            }
        }

        // Unselected tabs drawn first (behind panel edge)
        renderAllTabs(ctx, mouseX, mouseY, false);

        // Background clipped to hide the hotbar area at the bottom of the texture
        ctx.enableScissor(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_CLIP_HEIGHT);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED,
            Identifier.ofVanilla("textures/gui/container/creative_inventory/tab_items.png"),
            panelX, panelY, 0f, 0f, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
        ctx.disableScissor();

        // Search field only visible when the Search tab is active
        if (tabKeys.get(currentTabIndex).equals(SEARCH_KEY)) {
            searchField.render(ctx, mouseX, mouseY, delta);
        }

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
            ctx.drawItemTooltip(MinecraftClient.getInstance().textRenderer, entry.displayStack, mouseX, mouseY);
        }
    }

    private void renderAllTabs(DrawContext ctx, int mouseX, int mouseY, boolean selectedOnly) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        var groupRegistry = client.world.getRegistryManager().getOptional(RegistryKeys.ITEM_GROUP);
        if (groupRegistry.isEmpty()) return;

        for (int i = 0; i < tabKeys.size(); i++) {
            ItemGroup group = groupRegistry.get().get(tabKeys.get(i)); // null for UNCATEGORIZED_KEY
            boolean active = (i == currentTabIndex);
            if (active != selectedOnly) continue;
            renderTabIcon(ctx, group, active, i);
        }
    }

    private void renderTabIcon(DrawContext ctx, ItemGroup group, boolean active, int tabIndex) {
        // Use index-based layout to avoid conflicts with vanilla group column assignments
        boolean isTop = (tabIndex < 7);
        int col = isTop ? tabIndex : (tabIndex - 7);

        int tabX = panelX + col * TAB_COL_STEP;
        int tabY = isTop ? (panelY - TAB_Y_OFFSET) : (panelY + PANEL_CLIP_HEIGHT - 4);

        Identifier[] sprites = isTop
            ? (active ? TAB_TOP_SELECTED    : TAB_TOP_UNSELECTED)
            : (active ? TAB_BOTTOM_SELECTED : TAB_BOTTOM_UNSELECTED);
        Identifier sprite = sprites[MathHelper.clamp(col, 0, sprites.length - 1)];

        ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, sprite, tabX, tabY, TAB_W, TAB_H);

        int iconX = tabX + 5;
        int iconY = tabY + 8 + (isTop ? 1 : -1);
        ItemStack icon;
        if (group != null) {
            icon = group.getIcon();
        } else {
            RegistryKey<ItemGroup> key = tabKeys.get(tabIndex);
            icon = key.equals(SEARCH_KEY) ? new ItemStack(Items.COMPASS) : new ItemStack(Items.CHEST);
        }
        ctx.drawItem(icon, iconX, iconY);
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

        // Tab click (above or below panel)
        int tabIdx = getTabAt(mouseX, mouseY);
        if (tabIdx >= 0) {
            currentTabIndex = tabIdx;
            scrollOffset = 0;
            refreshSlots();
            // Auto-focus search field when switching to search tab, defocus otherwise
            if (tabKeys.get(tabIdx).equals(SEARCH_KEY)) {
                searchField.setFocused(true);
            } else {
                searchField.setFocused(false);
            }
            return true;
        }

        // Outside visible panel area: unfocus search and let vanilla handle
        if (mouseX < panelX || mouseX >= panelX + PANEL_WIDTH
                || mouseY < panelY || mouseY >= panelY + PANEL_CLIP_HEIGHT) {
            searchField.setFocused(false);
            return false;
        }

        if (searchField.mouseClicked(click, consumed)) return true;

        // If cursor is holding items, deposit them into storage instead of any other action
        if (button == 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
                if (!cursor.isEmpty()) {
                    ModMessaging.sendDeposit(
                        Registries.ITEM.getId(cursor.getItem()).toString(),
                        cursor.getCount()
                    );
                    return true;
                }
            }
        }

        // Scrollbar click
        if (button == 0) {
            int totalRows = (int) Math.ceil((double) currentSlots.size() / COLS);
            int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
            if (maxScroll > 0
                    && mouseX >= panelX + SCROLL_X && mouseX < panelX + SCROLL_X + SCROLL_W) {
                int trackTop    = panelY + SCROLL_AREA_TOP;
                int trackBottom = trackTop + SCROLL_AREA_H;
                if (mouseY >= trackTop && mouseY <= trackBottom) {
                    int thumbRange = SCROLL_AREA_H - SCROLL_H;
                    float ratio = (float)((int)mouseY - trackTop - SCROLL_H / 2) / thumbRange;
                    scrollOffset = MathHelper.clamp((int)(ratio * maxScroll), 0, maxScroll);
                    isScrollbarDragging = true;
                    return true;
                }
            }
        }

        // Panel drag from header bar (above item grid, excluding search box)
        if (button == 0 && isInDragHandle((int) mouseX, (int) mouseY)) {
            isDragging = true;
            dragLastX = (int) mouseX;
            dragLastY = (int) mouseY;
            return true;
        }

        // Slot click: withdraw from storage
        if (hoveredSlot >= 0 && hoveredSlot < currentSlots.size()) {
            StoragePanelHandler.SlotEntry entry = currentSlots.get(hoveredSlot);
            if (entry.isOwned()) {
                long win = MinecraftClient.getInstance().getWindow().getHandle();
                boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                             || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                int amount;
                if (shift && button == 0) {
                    // Shift+left: withdraw max stack to inventory
                    amount = (int) Math.min((long) entry.displayStack.getItem().getMaxCount(), entry.count);
                } else if (button == 1) {
                    // Right click: half of available count
                    amount = (int) Math.max(1L, entry.count / 2);
                } else {
                    // Left click: single item
                    amount = 1;
                }
                ModMessaging.sendWithdraw(entry.itemKey, amount, shift);
            }
            return true;
        }

        return false;
    }

    private int getTabAt(double mouseX, double mouseY) {
        for (int i = 0; i < tabKeys.size(); i++) {
            // Index-based layout — matches renderTabIcon exactly
            boolean isTop = (i < 7);
            int col = isTop ? i : (i - 7);
            int tabX = panelX + col * TAB_COL_STEP;
            int tabY = isTop ? (panelY - TAB_Y_OFFSET) : (panelY + PANEL_CLIP_HEIGHT - 4);
            if (mouseX >= tabX && mouseX < tabX + TAB_W && mouseY >= tabY && mouseY < tabY + TAB_H) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInDragHandle(int x, int y) {
        if (x < panelX || x >= panelX + PANEL_WIDTH) return false;
        if (y < panelY || y >= panelY + GRID_Y) return false;
        // Exclude search box region
        if (x >= panelX + 82 && x < panelX + 82 + 79
                && y >= panelY + 6 && y < panelY + 6 + 9) return false;
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Clean up drag states (also handled by GLFW poll in render, but be explicit here)
            isScrollbarDragging = false;
            if (isDragging) {
                isDragging = false;
                if (positionDirty) {
                    config.save();
                    positionDirty = false;
                }
            }
        }
        // If releasing over our panel, consume the event and deposit any cursor items
        if (mouseX >= panelX && mouseX < panelX + PANEL_WIDTH
                && mouseY >= panelY && mouseY < panelY + PANEL_CLIP_HEIGHT) {
            if (button == 0) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
                    if (!cursor.isEmpty()) {
                        ModMessaging.sendDeposit(
                            Registries.ITEM.getId(cursor.getItem()).toString(),
                            cursor.getCount()
                        );
                    }
                }
            }
            return true;
        }
        return false;
    }

    public void onClose() {
        if (positionDirty) {
            config.save();
            positionDirty = false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int gridTop    = panelY + GRID_Y;
        int gridBottom = gridTop + VISIBLE_ROWS * SLOT_SIZE;
        if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH || mouseY < gridTop || mouseY > gridBottom) {
            return false;
        }
        int totalRows = (int) Math.ceil((double) currentSlots.size() / COLS);
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollOffset = MathHelper.clamp(scrollOffset - (int) Math.signum(verticalAmount), 0, maxScroll);
        return true;
    }

    public boolean charTyped(CharInput charInput) {
        return searchField.charTyped(charInput);
    }

    // When search field is focused, consume all key presses to prevent accidental
    // inventory hotkey triggers. Escape unfocuses the field.
    public boolean keyPressed(KeyInput keyInput) {
        if (searchField.isFocused()) {
            if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchField.setFocused(false);
                return true;
            }
            searchField.keyPressed(keyInput);
            return true;
        }
        return false;
    }

    public void onStorageUpdate() {
        refreshSlots();
    }
}
