package dev.buildassist.plugin;

import dev.buildassist.plugin.db.StorageDatabase;
import dev.buildassist.plugin.menu.StorageMenuListener;
import dev.buildassist.plugin.network.PluginMessaging;
import dev.buildassist.plugin.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BuildAssistPlugin extends JavaPlugin {

    private StorageDatabase database;
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        database = new StorageDatabase(getDataFolder());
        database.initialize();

        storageManager = new StorageManager(database);

        getServer().getPluginManager().registerEvents(new StorageMenuListener(storageManager), this);

        PluginMessaging messaging = new PluginMessaging(this, storageManager);
        messaging.register();

        getLogger().info("BuildAssist enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("BuildAssist disabled.");
    }

    public StorageDatabase getDatabase() {
        return database;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}
