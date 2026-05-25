package dev.buildassist.mod.client.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class StorageKeybind {

    public static KeyBinding OPEN_CONFIG;

    public static void register() {
        OPEN_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "buildassist.keybind.open_config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT, // backtick ` as default for config
            new KeyBinding.Category(Identifier.of("buildassist", "keybind_category"))
        ));
    }
}
