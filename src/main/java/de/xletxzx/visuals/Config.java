package de.xletxzx.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("xletxzx_visuals.json");

    public static final String[] THEME_NAMES = {"Lila", "Blau", "Rot", "Grün", "Orange", "Pink", "Weiß"};
    public static final int[] THEME_COLORS = {0xFF8A5CFF, 0xFF3D8BFF, 0xFFFF4D5A, 0xFF3DDC84, 0xFFFF9A3D, 0xFFFF5CC8, 0xFFE8E8E8};
    public static final String[] BG_NAMES = {"Dunkel", "Farbverlauf", "Bild"};

    public int theme = 0;
    public int background = 1;
    public int bgIndex = 0;
    public boolean tutorial = true;
    public boolean mode3d = false;
    public boolean discord = true;
    public boolean onlineCount = true;
    public String clientId = "";

    private static Config instance = new Config();

    public static Config get() { return instance; }

    public int accent() { return THEME_COLORS[theme % THEME_COLORS.length]; }

    /** Anonymous random id used only for the online counter. */
    public String clientId() {
        if (clientId == null || clientId.length() != 32) {
            clientId = UUID.randomUUID().toString().replace("-", "");
            save();
        }
        return clientId;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) instance = GSON.fromJson(Files.readString(FILE), Config.class);
        } catch (Exception e) {
            instance = new Config();
        }
        if (instance == null) instance = new Config();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(instance)); } catch (Exception ignored) {}
    }
}
