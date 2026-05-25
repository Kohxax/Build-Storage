package dev.buildassist.mod;

import dev.buildassist.mod.network.StoragePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildAssistMod implements ModInitializer {

    public static final String MOD_ID = "buildassist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register payload types for both directions so Fabric accepts the channel
        PayloadTypeRegistry.playC2S().register(StoragePayload.ID, StoragePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StoragePayload.ID, StoragePayload.CODEC);
        LOGGER.info("BuildAssist initialized.");
    }
}
