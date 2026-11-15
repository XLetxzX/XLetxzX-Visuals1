package de.xletxzx.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class XLetxzXVisuals implements ClientModInitializer {
    public static final String MOD_ID = "xletxzx";
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        MenuScreen.snapshotMods();
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xletxzx.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new MenuScreen(null));
            }
        });
        if (Config.get().discord) DiscordRpc.start();
        OnlineCounter.start();
    }
}
