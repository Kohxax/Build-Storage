package dev.buildassist.mod.client;

import dev.buildassist.mod.client.config.BuildAssistConfig;
import dev.buildassist.mod.client.config.ConfigScreen;
import dev.buildassist.mod.client.keybind.StorageKeybind;
import dev.buildassist.mod.client.screen.StoragePanel;
import dev.buildassist.mod.network.ModMessaging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class BuildAssistClient implements ClientModInitializer {

    private static StoragePanel activePanel;
    private static Runnable activePanelListener;
    private static boolean pluginDetected = false;

    @Override
    public void onInitializeClient() {
        StorageKeybind.register();
        ModMessaging.registerReceiver();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            StorageCache.INSTANCE.clear();
            pluginDetected = false;
            closePanel();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (StorageKeybind.OPEN_CONFIG.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ConfigScreen(null));
                }
            }
        });
    }

    public static void onInventoryOpen(InventoryScreen screen) {
        closePanel();
        ModMessaging.sendOpenStorage();

        ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> {
            StoragePanel panel = activePanel;
            return panel == null || !panel.mouseClicked(click, false);
        });

        ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, hAmount, vAmount) -> {
            StoragePanel panel = activePanel;
            return panel == null || !panel.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
        });

        ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyInput) -> {
            StoragePanel panel = activePanel;
            return panel == null || !panel.keyPressed(keyInput);
        });

        ScreenMouseEvents.allowMouseRelease(screen).register((s, click) -> {
            StoragePanel panel = activePanel;
            if (panel == null) return true;
            return !panel.mouseReleased(click.x(), click.y(), click.button());
        });

        if (pluginDetected) {
            createPanel(screen);
        }
    }

    public static void onStorageContentsReceived() {
        pluginDetected = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (activePanel == null && mc.currentScreen instanceof InventoryScreen inv) {
            createPanel(inv);
        }
    }

    private static void createPanel(InventoryScreen screen) {
        activePanel = new StoragePanel(screen, BuildAssistConfig.get());
        activePanelListener = activePanel::onStorageUpdate;
        StorageCache.INSTANCE.addListener(activePanelListener);
    }

    public static void onInventoryClose() {
        closePanel();
    }

    private static void closePanel() {
        if (activePanel != null) {
            activePanel.onClose();
        }
        if (activePanelListener != null) {
            StorageCache.INSTANCE.removeListener(activePanelListener);
            activePanelListener = null;
        }
        activePanel = null;
    }

    public static StoragePanel getActivePanel() {
        return activePanel;
    }
}
