package dev.buildassist.mod.client;

import dev.buildassist.mod.client.config.BuildAssistConfig;
import dev.buildassist.mod.client.config.ConfigScreen;
import dev.buildassist.mod.client.keybind.StorageKeybind;
import dev.buildassist.mod.client.screen.StoragePanel;
import dev.buildassist.mod.network.ModMessaging;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class BuildAssistClient implements ClientModInitializer {

    private static StoragePanel activePanel;
    private static Runnable activePanelListener;

    @Override
    public void onInitializeClient() {
        StorageKeybind.register();
        ModMessaging.registerReceiver();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            StorageCache.INSTANCE.clear();
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
        activePanel = new StoragePanel(screen, BuildAssistConfig.get());
        activePanelListener = activePanel::onStorageUpdate;
        StorageCache.INSTANCE.addListener(activePanelListener);
    }

    public static void onInventoryClose() {
        closePanel();
    }

    private static void closePanel() {
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
