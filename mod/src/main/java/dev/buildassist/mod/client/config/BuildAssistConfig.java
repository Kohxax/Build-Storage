package dev.buildassist.mod.client.config;

import com.google.gson.*;
import dev.buildassist.mod.BuildAssistMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public class BuildAssistConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
        .resolve("buildassist.json");

    private PanelSide panelSide = PanelSide.RIGHT;
    private int inventoryOffsetX = 0;
    private int inventoryOffsetY = 0;
    private int panelOffsetX = 0;
    private int panelOffsetY = 0;

    private static BuildAssistConfig instance;

    public static BuildAssistConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static BuildAssistConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            BuildAssistConfig cfg = new BuildAssistConfig();
            cfg.save();
            return cfg;
        }
        try {
            String json = Files.readString(CONFIG_PATH);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            BuildAssistConfig cfg = new BuildAssistConfig();
            if (obj.has("panel_side")) {
                try { cfg.panelSide = PanelSide.valueOf(obj.get("panel_side").getAsString()); }
                catch (IllegalArgumentException ignored) {}
            }
            if (obj.has("inventory_offset_x")) cfg.inventoryOffsetX = obj.get("inventory_offset_x").getAsInt();
            if (obj.has("inventory_offset_y")) cfg.inventoryOffsetY = obj.get("inventory_offset_y").getAsInt();
            if (obj.has("panel_offset_x"))     cfg.panelOffsetX = obj.get("panel_offset_x").getAsInt();
            if (obj.has("panel_offset_y"))     cfg.panelOffsetY = obj.get("panel_offset_y").getAsInt();
            return cfg;
        } catch (Exception e) {
            BuildAssistMod.LOGGER.error("Failed to load config, using defaults", e);
            return new BuildAssistConfig();
        }
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("panel_side", panelSide.name());
            obj.addProperty("inventory_offset_x", inventoryOffsetX);
            obj.addProperty("inventory_offset_y", inventoryOffsetY);
            obj.addProperty("panel_offset_x", panelOffsetX);
            obj.addProperty("panel_offset_y", panelOffsetY);
            Files.writeString(CONFIG_PATH, new GsonBuilder().setPrettyPrinting().create().toJson(obj));
        } catch (IOException e) {
            BuildAssistMod.LOGGER.error("Failed to save config", e);
        }
    }

    // Getters
    public PanelSide getPanelSide()     { return panelSide; }
    public int getInventoryOffsetX()    { return inventoryOffsetX; }
    public int getInventoryOffsetY()    { return inventoryOffsetY; }
    public int getPanelOffsetX()        { return panelOffsetX; }
    public int getPanelOffsetY()        { return panelOffsetY; }

    // Setters (call save() after)
    public void setPanelSide(PanelSide v)    { this.panelSide = v; }
    public void setInventoryOffsetX(int v)   { this.inventoryOffsetX = v; }
    public void setInventoryOffsetY(int v)   { this.inventoryOffsetY = v; }
    public void setPanelOffsetX(int v)       { this.panelOffsetX = v; }
    public void setPanelOffsetY(int v)       { this.panelOffsetY = v; }
}
