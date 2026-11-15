package de.xletxzx.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Screen {
    private static final Identifier LOGO = Identifier.of("xletxzx", "textures/gui/logo.png");
    private static final String VERSION = "1.1";
    private static final String DISCORD_URL = "https://discord.gg/SkyTxT";

    private static final String[] TABS = {"HOME", "EXTRAS", "MODS"};
    private static final int[] TAB_PAGES = {0, 1, 4};
    private static final int HOME = 0, EXTRAS = 1, DESIGN = 2, CREDITS = 3, IMPORT = 4, SETTINGS = 5;

    private static final String[] LOOK_NAMES = {"Modern", "Minecraft", "Classic", "Glass", "Flat"};
    private static final String[] BG_NAMES = {
        "Winter night", "Gradient", "Dark", "Image",
        "Aurora", "Starfield", "Matrix rain", "Nether embers", "Ocean waves", "Cyber grid",
        "Bubbles", "Fireflies", "Rain", "Plasma", "Warp speed", "Snowfall", "Disco", "Pulse rings",
        "Meteors", "Sunset sky",
        "Midnight Blue", "Charcoal", "Forest", "Crimson", "Deep Ocean", "Sunset", "Violet Haze",
        "Mint", "Slate", "Sakura"
    };
    // static gradients, index = background index - 20
    private static final int[][] STATIC_BG = {
        {0xFF0B1030, 0xFF1B2A6B}, {0xFF1A1A1F, 0xFF2E2E36}, {0xFF0B2A14, 0xFF1F5A2C},
        {0xFF2A0508, 0xFF6B1018}, {0xFF021A2E, 0xFF0B4A6B}, {0xFF2B1038, 0xFFFF8A4D},
        {0xFF1E1038, 0xFF5B3A9E}, {0xFF0B2A2A, 0xFF2FB38E}, {0xFF20252E, 0xFF4A5566},
        {0xFF3A1230, 0xFFFF9EC8}
    };
    private static final String[] THEME_NAMES = {
        "Purple Night", "Classic Christmas", "Snowy Winter", "Christmas Night", "Golden Christmas",
        "Aurora Glass", "Sunset", "Deep Ocean", "Nether", "The End", "Forest", "Candy",
        "Midnight", "Terminal", "Paper", "Vanilla"
    };
    // {accent color, background, look}
    private static final int[][] THEME_VALS = {
        {0xFF8A5CFF, 0, 0}, {0xFFE8433F, 0, 0}, {0xFF9FD3FF, 0, 3}, {0xFF3DDC84, 0, 0},
        {0xFFFFC857, 0, 0}, {0xFF5CFFC8, 1, 3}, {0xFFFF8A4D, 1, 4}, {0xFF3D8BFF, 1, 0},
        {0xFFFF4D5A, 2, 1}, {0xFFB36BFF, 2, 3}, {0xFF3DDC84, 0, 4}, {0xFFFF5CC8, 1, 0},
        {0xFF5C7CFF, 2, 0}, {0xFF39FF14, 2, 4}, {0xFF444444, 2, 2}, {0xFFAAAAAA, 2, 1}
    };

    // {display name, mod id} - click opens the mod settings
    private static final String[][] FPS_MODS = {
        {"Sodium", "sodium"}, {"Sodium Extra", "sodium-extra"}, {"Entity Culling", "entityculling"},
        {"More Culling", "moreculling"}, {"Dynamic FPS", "dynamic_fps"}, {"ImmediatelyFast", "immediatelyfast"},
        {"Lithium", "lithium"}, {"FerriteCore", "ferritecore"}, {"Krypton", "krypton"},
        {"Bobby", "bobby"}, {"Clumps", "clumps"}, {"Smooth Scroll", "smoothscroll"}
    };
    private static final String[][] VISUAL_MODS = {
        {"Skin Layers 3D", "skinlayers3d"}, {"Wavey Capes", "waveycapes"}, {"More Cosmetics", "morecosmetics"},
        {"Skyboxify", "skyboxify"}, {"LambDynamicLights", "lambdynlights"}, {"Item Physic", "itemphysic"},
        {"Natural Motion Blur", "naturalmotionblur"}, {"No Texture Rotations", "notexturerotations"},
        {"Block Highlight", "custom-block-highlight"}, {"Not Enough Animations", "notenoughanimations"},
        {"Zoomify", "zoomify"}, {"Freelook", "freelook"}, {"Gamma Utils", "gammautils"},
        {"Toggle Name Tags", "togglenametags"}, {"Creamy Keys", "creamykeys"}
    };
    private static final String[] CAT_NAMES = {"FPS & PERFORMANCE", "VISUALS"};
    private static final int[] CAT_COLORS = {0xFF3DDC84, 0xFF8A5CFF};
    private static final String[][][] CATS = {FPS_MODS, VISUAL_MODS};

    // Mods whose settings live inside the Sodium video settings screen
    private static final java.util.Set<String> SODIUM_PAGES = java.util.Set.of(
        "sodium", "sodium-extra", "moreculling", "bobby", "notexturerotations");

    // styled button record (drawn by us, clicked through an invisible vanilla button)
    private static class Btn {
        int x, y, w, h, kind, look;
        String label;
        boolean selected;
    }

    private static final java.util.Set<String> startupMods = new java.util.HashSet<>();

    /** Remembers which jars were in the mods folder when the game started (to flag new ones after a rescan). */
    public static void snapshotMods() {
        try (java.util.stream.Stream<Path> st = Files.list(FabricLoader.getInstance().getGameDir().resolve("mods"))) {
            st.forEach(p -> startupMods.add(p.getFileName().toString()));
        } catch (Exception ignored) {
        }
    }

    private final List<Btn> btns = new ArrayList<>();
    private final int[] secY = new int[4];
    private int W, H, OX, OY;
    private int bgPage = 0;
    private float rawMx, rawMy;
    private static final String MADE = "MADE BY XLETXZX WITH LOVE";
    private final List<int[]> headers = new ArrayList<>();
    private List<String> modFiles = new ArrayList<>();
    private TextFieldWidget searchField;
    private TextFieldWidget linkField;
    private String search = "";
    private int scroll = 0;
    private int maxScroll = 0;
    private int listTop = 0, listBottom = 0;
    private final Screen parent;
    private int page = HOME;
    private String note = "";
    private long noteUntil = 0;

    public MenuScreen(Screen parent) {
        super(Text.literal("XLetxzX Visuals"));
        this.parent = parent;
    }

    // ---- settings stored in the existing config fields: theme = theme index,
    // ---- background = look * 10 + background mode
    private static int accent() { return THEME_VALS[Config.get().theme % THEME_VALS.length][0]; }
    private static int look() { return Math.floorMod(Config.get().background / 10, 5); }
    private static int bgMode() { return Math.floorMod(Config.get().bgIndex, BG_NAMES.length); }
    private static void setLook(int l) { Config.get().background = l * 10; Config.save(); }
    private static void setBg(int b) { Config.get().bgIndex = b; Config.save(); }

    private void showNote(String s) {
        note = s;
        noteUntil = System.currentTimeMillis() + 5000;
    }

    private void openConfig(String name, String modId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!modId.equals("sodium") && !modId.equals("sodium-extra")) {
            try {
                Class<?> c = Class.forName("com.terraformersmc.modmenu.ModMenu");
                Object s = c.getMethod("getConfigScreen", String.class, Screen.class).invoke(null, modId, this);
                if (s instanceof Screen scr) {
                    mc.setScreen(scr);
                    return;
                }
            } catch (Throwable t) {
                System.err.println("[XLetxzX] config screen error for " + modId + ": " + t);
            }
        }
        if (SODIUM_PAGES.contains(modId)) {
            try {
                mc.setScreen(new VideoOptionsScreen(this, mc, mc.options));
                return;
            } catch (Throwable t) {
                System.err.println("[XLetxzX] video settings error: " + t);
            }
        }
        showNote(name + " has no in-game settings screen (config file only).");
    }

    /** Opens the vanilla keybind screen (the XLetxzX menu key is listed under "Miscellaneous"). */
    private void openKeybinds() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new KeybindsScreen(this, mc.options));
        } catch (Throwable t) {
            showNote("Could not open the keybind screen.");
        }
    }

    private void openModList() {
        try {
            Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            Object s = c.getConstructor(Screen.class).newInstance(this);
            if (s instanceof Screen scr) {
                MinecraftClient.getInstance().setScreen(scr);
                return;
            }
        } catch (Throwable ignored) {
        }
        showNote("Could not open Mod Menu.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setLastEnum(SimpleOption opt) {
        try {
            Object cur = opt.getValue();
            if (cur instanceof Enum<?> e) {
                Object[] all = e.getDeclaringClass().getEnumConstants();
                opt.setValue(all[all.length - 1]);
            }
        } catch (Throwable ignored) {
        }
    }

    private void applyFpsPreset() {
        try {
            GameOptions o = MinecraftClient.getInstance().options;
            o.getViewDistance().setValue(8);
            o.getSimulationDistance().setValue(6);
            setLastEnum(o.getParticles());
            o.getEntityDistanceScaling().setValue(0.5);
            o.getMaxFps().setValue(260);
            o.getEnableVsync().setValue(false);
            o.getBiomeBlendRadius().setValue(1);
            o.write();
            showNote("FPS boost applied (render distance 8, minimal particles, VSync off).");
        } catch (Throwable t) {
            showNote("Could not apply FPS boost.");
        }
    }

    private Btn add(int kind, int x, int y, int w, int h, String label, boolean sel, Runnable r) {
        Btn b = new Btn();
        b.kind = kind; b.x = x; b.y = y; b.w = w; b.h = h; b.label = label; b.selected = sel;
        btns.add(b);
        addDrawableChild(ButtonWidget.builder(Text.empty(), btn -> r.run()).dimensions(x + OX, y + OY, w, h).build());
        return b;
    }

    private void applyTheme(int i) {
        Config c = Config.get();
        c.theme = i;
        c.background = THEME_VALS[i][2] * 10;
        c.bgIndex = THEME_VALS[i][1];
        Config.save();
        clearAndInit();
    }

    private void openModsFolder() {
        try {
            Files.createDirectories(modsDir());
            net.minecraft.util.Util.getOperatingSystem().open(modsDir().toUri());
        } catch (Throwable t) {
            showNote("Could not open the folder: " + t.getMessage());
        }
    }

    private void computeWindow() {
        int sw = this.width, sh = this.height;
        W = Math.min(sw - 8, Math.max(420, Math.min(640, sw * 70 / 100)));
        H = Math.min(sh - 8, Math.max(300, Math.min(460, sh * 80 / 100)));
        OX = (sw - W) / 2;
        OY = (sh - H) / 2;
    }

    private int madeWidth() { return textRenderer.getWidth(MADE) + 26; }

    private Path modsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    private static boolean isFabricMod(Path p) {
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(p.toFile())) {
            return z.getEntry("fabric.mod.json") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> listMods() {
        try (java.util.stream.Stream<Path> st = Files.list(modsDir())) {
            List<String> out = new ArrayList<>();
            st.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.toLowerCase().endsWith(".jar")) out.add(n);
            });
            java.util.Collections.sort(out);
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Drag & drop of .jar files onto the window copies them into the mods folder. */
    @Override
    public void onFilesDropped(List<Path> paths) {
        int ok = 0;
        String last = "";
        for (Path p : paths) {
            String n = p.getFileName().toString();
            if (!n.toLowerCase().endsWith(".jar")) { showNote("Only .jar mod files can be imported."); continue; }
            if (!isFabricMod(p)) { showNote(n + " is not a Fabric mod."); continue; }
            try {
                Files.createDirectories(modsDir());
                Files.copy(p, modsDir().resolve(n), StandardCopyOption.REPLACE_EXISTING);
                ok++;
                last = n;
            } catch (Exception e) {
                showNote("Import failed: " + e.getMessage());
            }
        }
        if (ok > 0) {
            showNote(ok == 1 ? "Imported " + last + " - restart the game to load it."
                    : "Imported " + ok + " mods - restart the game to load them.");
            page = IMPORT;
            clearAndInit();
        }
    }

    /** Downloads a mod from a direct https link into the mods folder. */
    private void downloadMod(String raw) {
        final String link = raw == null ? "" : raw.trim();
        if (link.isEmpty()) { showNote("Paste a direct download link first."); return; }
        showNote("Downloading...");
        Thread th = new Thread(() -> {
            String msg;
            try {
                URI u = URI.create(link);
                if (!"https".equalsIgnoreCase(u.getScheme())) throw new IllegalStateException("the link must start with https://");
                HttpClient hc = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest req = HttpRequest.newBuilder(u).header("User-Agent", "XLetxzX-Visuals").build();
                HttpResponse<InputStream> resp = hc.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) throw new IllegalStateException("server answered " + resp.statusCode());
                String path = resp.uri().getPath();
                String fn = path.substring(path.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._+-]", "_");
                if (!fn.toLowerCase().endsWith(".jar")) throw new IllegalStateException("the link must point to a .jar file");
                Path dir = modsDir();
                Files.createDirectories(dir);
                Path tmp = dir.resolve(fn + ".part");
                long max = 100L * 1024 * 1024;
                try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(tmp)) {
                    byte[] buf = new byte[8192];
                    long total = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > max) throw new IllegalStateException("file is too big");
                        out.write(buf, 0, n);
                    }
                }
                if (!isFabricMod(tmp)) {
                    Files.deleteIfExists(tmp);
                    throw new IllegalStateException("that file is not a Fabric mod");
                }
                Files.move(tmp, dir.resolve(fn), StandardCopyOption.REPLACE_EXISTING);
                msg = "Imported " + fn + " - restart the game to load it.";
            } catch (Exception e) {
                msg = "Download failed: " + e.getMessage();
            }
            final String m = msg;
            MinecraftClient.getInstance().execute(() -> {
                showNote(m);
                if (page == IMPORT) clearAndInit();
            });
        }, "XLetxzX-Import");
        th.setDaemon(true);
        th.start();
    }

    private int contentHeight(int cols, String q) {
        int h = 0;
        for (int c = 0; c < CATS.length; c++) {
            int n = 0;
            for (String[] m : CATS[c]) if (m[0].toLowerCase().contains(q)) n++;
            if (n == 0) continue;
            h += 20 + ((n + cols - 1) / cols) * 30 + 6;
        }
        return h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (page == EXTRAS) {
            scroll = (int) Math.max(0, scroll - verticalAmount * 28);
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void init() {
        computeWindow();
        btns.clear();
        headers.clear();
        searchField = null;
        linkField = null;
        Config c = Config.get();
        int areaW = W - 40;

        // tabs
        int tabW = 70;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            add(5, 150 + i * (tabW + 4), 5, tabW, 18, TABS[i], page == TAB_PAGES[i], () -> { page = TAB_PAGES[idx]; scroll = 0; clearAndInit(); });
        }
        // import icon (top right)
        add(8, W - 28, 5, 20, 18, "Settings", page == SETTINGS, () -> { page = SETTINGS; clearAndInit(); });

        if (page == HOME) {
            int img = Math.min(H - 90, (W - 176) / 3);
            int regionStart = 20 + img + 20;
            int regionEnd = W - 176 - 10;
            int bx = (regionStart + regionEnd) / 2 - 95;
            int gy = H < 300 ? 25 : 28;
            int bh = H < 300 ? 20 : 22;
            int by = Math.max(80, H / 2 - 10 - (H < 300 ? 28 : 0));
            add(1, bx, by, 190, bh, "Continue", false, this::close);
            add(0, bx, by + gy, 190, bh, "Keybinds", false, this::openKeybinds);
            add(0, bx, by + gy * 2, 190, bh, "My mods", false, () -> { page = EXTRAS; scroll = 0; clearAndInit(); });
            add(0, bx, by + gy * 3, 190, bh, "Customize the look", false, () -> { page = DESIGN; clearAndInit(); });
            add(0, bx, by + gy * 4, 190, bh, "Credits", false, () -> { page = CREDITS; clearAndInit(); });
        } else if (page == DESIGN) {
            add(0, 20, 36, 60, 16, "< Back", false, () -> { page = HOME; clearAndInit(); });
            int cardH = H < 300 ? 40 : 56;
            int gap = 8;
            int cw = Math.min(120, (areaW - gap * 4) / 5);
            secY[0] = 62;
            for (int i = 0; i < 5; i++) {
                final int l = i;
                Btn b = add(3, 20 + i * (cw + gap), 76, cw, cardH, LOOK_NAMES[i], look() == i,
                        () -> { setLook(l); clearAndInit(); });
                b.look = i;
            }
            int y = 76 + cardH + 14;
            secY[1] = y;
            y += 14;
            int x = 20;
            for (int i = 0; i < THEME_NAMES.length; i++) {
                final int ti = i;
                int w = textRenderer.getWidth(THEME_NAMES[i]) + 16;
                if (x + w > 20 + areaW) { x = 20; y += 22; }
                boolean sel = c.theme == i && look() == THEME_VALS[i][2] && bgMode() == THEME_VALS[i][1];
                add(2, x, y, w, 18, THEME_NAMES[i], sel, () -> applyTheme(ti));
                x += w + 6;
            }
            y += 30;
            secY[2] = y;
            final int per = 12;
            final int pages = (BG_NAMES.length + per - 1) / per;
            bgPage = Math.max(0, Math.min(bgPage, pages - 1));
            add(0, W - 20 - 40, y - 3, 18, 16, "<", false, () -> { bgPage = Math.floorMod(bgPage - 1, pages); clearAndInit(); });
            add(0, W - 20 - 18, y - 3, 18, 16, ">", false, () -> { bgPage = (bgPage + 1) % pages; clearAndInit(); });
            y += 14;
            x = 20;
            for (int i = bgPage * per; i < Math.min(BG_NAMES.length, (bgPage + 1) * per); i++) {
                final int bi = i;
                int w = textRenderer.getWidth(BG_NAMES[i]) + 16;
                if (x + w > 20 + areaW) { x = 20; y += 22; }
                add(2, x, y, w, 18, BG_NAMES[i], bgMode() == i, () -> { setBg(bi); clearAndInit(); });
                x += w + 6;
            }
        } else if (page == SETTINGS) {
            add(0, 20, 36, 60, 16, "< Back", false, () -> { page = HOME; clearAndInit(); });
            add(0, 20, 78, 200, 20, "Customize the look", false, () -> { page = DESIGN; clearAndInit(); });
            add(0, 20, 104, 200, 20, "Keybinds", false, this::openKeybinds);
            add(0, 20, 130, 200, 20, "Credits", false, () -> { page = CREDITS; clearAndInit(); });
            add(2, 20, 186, 200, 18, "Discord status: " + (c.discord ? "ON" : "OFF"), c.discord, () -> {
                c.discord = !c.discord;
                if (c.discord) DiscordRpc.start(); else DiscordRpc.stop();
                Config.save(); clearAndInit();
            });
            add(2, 20, 236, 200, 18, "Online counter: " + (c.onlineCount ? "ON" : "OFF"), c.onlineCount, () -> {
                c.onlineCount = !c.onlineCount;
                Config.save();
                OnlineCounter.refresh();
                clearAndInit();
            });
            add(2, 230, 78, 180, 20, "Tutorial: " + (c.tutorial ? "ON" : "OFF"), c.tutorial, () -> {
                c.tutorial = !c.tutorial;
                Config.save(); clearAndInit();
            });
            add(2, 230, 104, 180, 20, "3D mode: " + (c.mode3d ? "ON" : "OFF"), c.mode3d, () -> {
                c.mode3d = !c.mode3d;
                Config.save(); clearAndInit();
            });
        } else if (page == CREDITS) {
            add(0, 20, H - 50, 120, 18, "Back", false, () -> { page = HOME; clearAndInit(); });
        } else if (page == IMPORT) {
            add(0, 20, 36, 60, 16, "< Back", false, () -> { page = HOME; clearAndInit(); });
            modFiles = listMods();
            linkField = new TextFieldWidget(textRenderer, 20 + OX, 190 + OY, areaW - 110, 18, Text.literal("Link"));
            linkField.setMaxLength(500);
            linkField.setPlaceholder(Text.literal("https://cdn.modrinth.com/.../mod.jar"));
            addDrawableChild(linkField);
            add(1, W - 20 - 100, 190, 100, 18, "Download", false, () -> downloadMod(linkField.getText()));
            add(0, W - 20 - 90, 232, 90, 16, "Rescan", false, () -> {
                List<String> now = listMods();
                int fresh = 0;
                for (String f : now) if (!startupMods.contains(f)) fresh++;
                showNote("Rescanned: " + now.size() + " mods in your folder"
                        + (fresh > 0 ? ", " + fresh + " new (restart to load)." : "."));
                clearAndInit();
            });
            add(1, W - 20 - 130, 36, 130, 16, "Open mods folder", false, this::openModsFolder);
            int maxRows = Math.max(1, (H - 24 - 252) / 11);
            for (int i = 0; i < Math.min(maxRows, modFiles.size()); i++) {
                add(9, 20, 251 + i * 11, areaW, 11, "", false, this::openModsFolder);
            }
        } else {
            // EXTRAS: all mods in one list
            int top = 64;
            int bottom = H - 30;
            listTop = top;
            listBottom = bottom;
            searchField = new TextFieldWidget(textRenderer, 20 + OX, 38 + OY, 190, 18, Text.literal("Search"));
            searchField.setMaxLength(40);
            searchField.setPlaceholder(Text.literal("Search mods..."));
            searchField.setText(search);
            searchField.setChangedListener(t -> {
                if (!t.equals(search)) {
                    search = t;
                    scroll = 0;
                    clearAndInit();
                }
            });
            addDrawableChild(searchField);
            if (!search.isEmpty()) setInitialFocus(searchField);
            add(1, 220, 38, 100, 18, "FPS boost", false, this::applyFpsPreset);
            add(0, 326, 38, 100, 18, "Mod Menu", false, this::openModList);

            int cols = Math.max(2, Math.min(4, areaW / 150));
            int gap = 6;
            int cw = (areaW - gap * (cols - 1)) / cols;
            int ch = 24;
            String q = search.toLowerCase().trim();
            maxScroll = Math.max(0, contentHeight(cols, q) - (bottom - top));
            scroll = Math.min(scroll, maxScroll);
            int y = top - scroll;
            for (int cat = 0; cat < CATS.length; cat++) {
                List<String[]> vis = new ArrayList<>();
                for (String[] m : CATS[cat]) if (m[0].toLowerCase().contains(q)) vis.add(m);
                if (vis.isEmpty()) continue;
                if (y >= top - 2 && y + 14 <= bottom) headers.add(new int[]{y, cat});
                y += 20;
                for (int i = 0; i < vis.size(); i++) {
                    final String name = vis.get(i)[0];
                    final String id = vis.get(i)[1];
                    int x = 20 + (i % cols) * (cw + gap);
                    int yy = y + (i / cols) * (ch + gap);
                    if (yy >= top && yy + ch <= bottom) {
                        Btn b = add(7, x, yy, cw, ch, name, false, () -> openConfig(name, id));
                        b.look = cat;
                    }
                }
                y += ((vis.size() + cols - 1) / cols) * (ch + gap) + 6;
            }
        }

        add(4, 6, H - 20, 74, 16, "Discord", false, () -> ConfirmLinkScreen.open(this, DISCORD_URL));
        add(9, 84, H - 20, madeWidth(), 16, "", false, () -> ConfirmLinkScreen.open(this, DISCORD_URL));
        add(0, W - 66, H - 20, 60, 16, "Close", false, this::close);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // custom background is drawn in render()
    }

    private void drawCentered(DrawContext ctx, String s, int cx, int y, int color) {
        ctx.drawText(textRenderer, Text.literal(s), cx - textRenderer.getWidth(s) / 2, y, color, true);
    }

    private static int[] palette(int look, boolean hov) {
        return switch (look) {
            case 1 -> new int[]{hov ? 0xFF7A7A8A : 0xFF6B6B6B, 0xFF000000, 0xFFFFFFFF};
            case 2 -> new int[]{hov ? 0xFFE0E0E0 : 0xFFC8C8C8, 0xFF333333, 0xFF202020};
            case 3 -> new int[]{hov ? 0x88FFFFFF : 0x55FFFFFF, 0x99FFFFFF, 0xFFFFFFFF};
            case 4 -> new int[]{hov ? 0xFF2C2C38 : 0xFF1E1E26, 0xFF1E1E26, 0xFFFFFFFF};
            default -> new int[]{hov ? 0xFF2E2E3C : 0xFF23232E, 0xFF3A3A4C, 0xFFFFFFFF};
        };
    }

    private void box(DrawContext ctx, int x, int y, int w, int h, int border, int bg, boolean round) {
        if (round) {
            ctx.fill(x + 1, y, x + w - 1, y + h, border);
            ctx.fill(x, y + 1, x + w, y + h - 1, border);
        } else {
            ctx.fill(x, y, x + w, y + h, border);
        }
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
    }

    private void drawBtn(DrawContext ctx, Btn b, int mx, int my) {
        boolean hov = mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h;
        int acc = accent();
        int look = look();
        if (b.kind == 9) {
            if (hov) ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, 0x22FFFFFF);
            return;
        }
        if (b.kind == 3) {
            drawCard(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 6) {
            drawImportIcon(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 7) {
            drawModCard(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 8) {
            drawGearIcon(ctx, b, hov, acc);
            return;
        }
        boolean d3 = Config.get().mode3d && (b.kind == 0 || b.kind == 1 || b.kind == 2 || b.kind == 4);
        int origY = b.y;
        if (d3 && hov) b.y -= 2;
        int bg, border, text;
        boolean round = look != 1 && look != 2;
        if (b.kind == 4) {
            bg = hov ? 0xFF6D79F5 : 0xFF5865F2; border = bg; text = 0xFFFFFFFF;
        } else if (b.kind == 1) {
            bg = hov ? (acc | 0xFF303030) : acc; border = acc; text = 0xFFFFFFFF;
        } else if (b.kind == 2) {
            bg = hov ? 0xFF262A3C : 0xFF1C1F2E; border = (b.selected || hov) ? acc : 0xFF3B4056; text = 0xFFDDDDEE;
        } else if (b.kind == 5) {
            bg = hov ? 0xFF262A3C : 0x00000000; border = 0x00000000; text = b.selected ? 0xFFFFFFFF : 0xFFAAAACC;
        } else {
            int[] p = palette(look, hov);
            bg = p[0]; border = p[1]; text = p[2];
        }
        if (b.kind == 5) {
            if (hov || b.selected) ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, hov ? 0x33FFFFFF : 0x22FFFFFF);
            if (b.selected) ctx.fill(b.x, b.y + b.h - 2, b.x + b.w, b.y + b.h, acc);
        } else {
            if (d3) ctx.fill(b.x, b.y + 3, b.x + b.w, b.y + b.h + 3, 0xFF05050A);
            if (b.kind != 4) ctx.fill(b.x + 1, b.y + 2, b.x + b.w + 1, b.y + b.h + 2, 0x44000000);
            if (b.kind == 0 && b.selected) border = acc;
            if (b.kind == 1 && hov) ctx.fill(b.x - 1, b.y - 1, b.x + b.w + 1, b.y + b.h + 1, (acc & 0x00FFFFFF) | 0x55000000);
            box(ctx, b.x, b.y, b.w, b.h, border, bg, round);
            if (look == 1 && b.kind == 0) ctx.fill(b.x + 1, b.y + 1, b.x + b.w - 1, b.y + 2, 0x55FFFFFF);
            if (look == 4 && b.kind == 0 && hov) ctx.fill(b.x + 1, b.y + b.h - 2, b.x + b.w - 1, b.y + b.h - 1, acc);
            if (b.kind == 1) ctx.fill(b.x + 2, b.y + 1, b.x + b.w - 2, b.y + 2, 0x55FFFFFF);
        }
        int tw = textRenderer.getWidth(b.label);
        int tx = b.x + (b.w - tw) / 2;
        if (b.kind == 4) {
            int ix = b.x + (b.w - (16 + tw)) / 2;
            drawDiscordIcon(ctx, ix, b.y + (b.h - 9) / 2);
            tx = ix + 16;
        }
        ctx.drawText(textRenderer, Text.literal(b.label), tx, b.y + (b.h - 8) / 2, text, b.kind == 1);
        b.y = origY;
    }

    /** Small pixel-art Discord face. */
    private void drawDiscordIcon(DrawContext ctx, int x, int y) {
        int w = 0xFFFFFFFF, eye = 0xFF5865F2;
        ctx.fill(x + 1, y, x + 11, y + 1, w);
        ctx.fill(x, y + 1, x + 12, y + 7, w);
        ctx.fill(x + 2, y + 7, x + 5, y + 8, w);
        ctx.fill(x + 7, y + 7, x + 10, y + 8, w);
        ctx.fill(x + 1, y + 8, x + 3, y + 9, w);
        ctx.fill(x + 9, y + 8, x + 11, y + 9, w);
        ctx.fill(x + 2, y + 3, x + 4, y + 5, eye);
        ctx.fill(x + 8, y + 3, x + 10, y + 5, eye);
    }

    private void drawImportIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, hov ? 0xFF262A3C : 0xFF1C1F2E, true);
        int cx = b.x + b.w / 2;
        int col = 0xFFFFFFFF;
        ctx.fill(cx - 1, b.y + 3, cx + 1, b.y + 9, col);
        for (int i = 0; i < 4; i++) ctx.fill(cx - 3 + i, b.y + 8 + i, cx + 3 - i, b.y + 9 + i, col);
        ctx.fill(b.x + 4, b.y + b.h - 5, b.x + b.w - 4, b.y + b.h - 4, col);
        ctx.fill(b.x + 4, b.y + b.h - 8, b.x + 5, b.y + b.h - 4, col);
        ctx.fill(b.x + b.w - 5, b.y + b.h - 8, b.x + b.w - 4, b.y + b.h - 4, col);
        if (hov) {
            int tw = textRenderer.getWidth(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, Text.literal(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    private void drawGearIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        int bg = hov ? 0xFF262A3C : 0xFF1C1F2E;
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, bg, true);
        int cx = b.x + b.w / 2, cy = b.y + b.h / 2;
        int col = 0xFFFFFFFF;
        ctx.fill(cx - 4, cy - 4, cx + 4, cy + 4, col);
        ctx.fill(cx - 1, cy - 6, cx + 1, cy + 6, col);
        ctx.fill(cx - 6, cy - 1, cx + 6, cy + 1, col);
        ctx.fill(cx - 5, cy - 5, cx - 3, cy - 3, col);
        ctx.fill(cx + 3, cy - 5, cx + 5, cy - 3, col);
        ctx.fill(cx - 5, cy + 3, cx - 3, cy + 5, col);
        ctx.fill(cx + 3, cy + 3, cx + 5, cy + 5, col);
        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, bg);
        if (hov) {
            int tw = textRenderer.getWidth(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, Text.literal(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    private void drawModCard(DrawContext ctx, Btn b, boolean hov, int acc) {
        int cat = CAT_COLORS[Math.floorMod(b.look, CAT_COLORS.length)];
        if (Config.get().mode3d) ctx.fill(b.x, b.y + 3, b.x + b.w, b.y + b.h + 3, 0xFF05050A);
        ctx.fill(b.x + 1, b.y + 2, b.x + b.w + 1, b.y + b.h + 2, 0x44000000);
        box(ctx, b.x, b.y, b.w, b.h, hov ? acc : 0xFF2B2F42, hov ? 0xFF1F2336 : 0xFF161926, true);
        ctx.fill(b.x + 1, b.y + 3, b.x + 4, b.y + b.h - 3, cat);
        String label = b.label;
        int maxW = b.w - 30;
        while (label.length() > 3 && textRenderer.getWidth(label) > maxW) label = label.substring(0, label.length() - 2);
        if (!label.equals(b.label)) label = label + "..";
        ctx.drawText(textRenderer, Text.literal(label), b.x + 10, b.y + (b.h - 8) / 2, 0xFFE6E6F2, false);
        ctx.drawText(textRenderer, Text.literal("\u203A"), b.x + b.w - 12, b.y + (b.h - 8) / 2,
                hov ? acc : 0xFF666680, false);
    }

    private void drawCard(DrawContext ctx, Btn b, boolean hov, int acc) {
        int l = b.look;
        int[] panel = {0xFF1A1A22, 0xFF3A3A3A, 0xFFC8C8C8, 0xFF262640, 0xFF16161C};
        boolean dark = l != 2;
        int border = b.selected ? 0xFFFFFFFF : (hov ? acc : 0xFF2A2A36);
        box(ctx, b.x, b.y, b.w, b.h, border, panel[l], true);
        int lineCol = dark ? 0xFFDDDDEE : 0xFF555555;
        ctx.fill(b.x + 8, b.y + 8, b.x + 8 + b.w / 3, b.y + 10, lineCol);
        ctx.fill(b.x + 8, b.y + 13, b.x + 8 + b.w / 2, b.y + 14, dark ? 0xFF888899 : 0xFF888888);
        int[] p = palette(l, false);
        int bx = b.x + b.w / 5, bw = b.w * 3 / 5, by = b.y + b.h / 2 - 4, bh = Math.max(8, b.h / 5);
        if (l == 0) {
            ctx.fill(bx, by, bx + bw, by + bh, 0xFF8A6CFF);
        } else if (l == 4) {
            ctx.fill(bx, by, bx + bw, by + bh, 0xFF5C8DEB);
        } else if (l == 3) {
            ctx.fill(bx, by, bx + bw, by + bh, 0xFFC8C8D8);
        } else {
            box(ctx, bx, by, bw, bh, p[1], p[0], false);
        }
        ctx.drawText(textRenderer, Text.literal(b.label), b.x + (b.w - textRenderer.getWidth(b.label)) / 2,
                b.y + b.h - 12, dark ? 0xFFFFFFFF : 0xFF202020, false);
    }

    private void drawSection(DrawContext ctx, String label, int y) { drawSection(ctx, label, y, W - 20); }

    private void drawSection(DrawContext ctx, String label, int y, int endX) {
        ctx.drawText(textRenderer, Text.literal(label), 20, y, 0xFF8888AA, false);
        ctx.fill(20 + textRenderer.getWidth(label) + 8, y + 4, endX, y + 5, 0x33FFFFFF);
    }

    private void drawTree(DrawContext ctx, int cx, int baseY, int h, int color) {
        ctx.fill(cx - Math.max(1, h / 14), baseY, cx + Math.max(1, h / 14) + 1, baseY + 8, 0xFF2A1A12);
        for (int row = 0; row < h; row++) {
            float f = row / (float) h;
            float local = (f * 3f) % 1f;
            int half = (int) ((0.25f + 0.75f * local) * (h * 0.12f + f * h * 0.22f));
            ctx.fill(cx - half, baseY - h + row, cx + half + 1, baseY - h + row + 1, color);
        }
    }

    private void drawWinter(DrawContext ctx, long t) {
        ctx.fillGradient(0, 0, W, H, 0xFF1B2250, 0xFF0A0E22);
        int mr = Math.max(18, H / 9);
        int mx = (int) (W * 0.38), my = (int) (H * 0.38);
        for (int dy = -mr; dy <= mr; dy++) {
            int dx = (int) Math.sqrt(mr * mr - dy * dy);
            ctx.fill(mx - dx, my + dy, mx + dx, my + dy + 1, 0x22FFFFFF);
        }
        for (int i = 0; i < 60; i++) {
            int x = (int) (((i * 7919) % 1000) / 1000f * W);
            int y = (int) (((i * 104729) % 1000) / 1000f * H * 0.7f);
            int a = 0x55 + (int) (0x44 * Math.sin(t / 700.0 + i));
            ctx.fill(x, y, x + 1, y + 1, (a << 24) | 0x00FFFFFF);
        }
        int n = 9, base = H - 24;
        for (int i = 0; i < n; i++) {
            int cx = (int) (W * (i + 1f) / n);
            int h = (int) (H * (0.2f + ((i * 53) % 25) / 100f) * 1.15f);
            drawTree(ctx, cx, base, h, 0xFF111A33);
        }
        for (int i = 0; i < n; i++) {
            int cx = (int) (W * (i + 0.5f) / n) + ((i * 37) % 20 - 10);
            int h = (int) (H * (0.18f + ((i * 53) % 25) / 100f));
            drawTree(ctx, cx, base, h, 0xFF0A0E1A);
            int top = base - h;
            ctx.fill(cx - 2, top, cx + 3, top + 1, 0xAAFFD27A);
            ctx.fill(cx, top - 2, cx + 1, top + 3, 0xAAFFD27A);
        }
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h, int acc) {
        ctx.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x55000000);
        box(ctx, x, y, w, h, 0x55FFFFFF, 0xDD0D0F1A, true);
        ctx.fill(x + 2, y, x + w - 2, y + 2, acc);
    }

    // ------------------------------------------------------------------
    // Backgrounds
    // ------------------------------------------------------------------
    private static float hash(int n) {
        int x = n * 374761393 + 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        x ^= (x >>> 16);
        return (x & 0xFFFF) / 65535f;
    }

    private static int hsv(double h, double s, double v, int a) {
        h = h - Math.floor(h);
        double c = v * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = v - c;
        double r, g, b;
        switch ((int) (h * 6)) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return ((a & 0xFF) << 24) | ((int) ((r + m) * 255) << 16) | ((int) ((g + m) * 255) << 8) | (int) ((b + m) * 255);
    }

    private void disc(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            ctx.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private void drawBackground(DrawContext ctx, long t, int acc) {
        double sec = t / 1000.0;
        int bg = bgMode();
        if (bg >= 20) {
            int[] pair = STATIC_BG[Math.min(bg - 20, STATIC_BG.length - 1)];
            ctx.fillGradient(0, 0, W, H, pair[0], pair[1]);
            return;
        }
        switch (bg) {
            case 0 -> drawWinter(ctx, t);
            case 1 -> ctx.fillGradient(0, 0, W, H, (acc & 0x00FFFFFF) | 0xFF000000, 0xFF06060A);
            case 2 -> ctx.fill(0, 0, W, H, 0xFF101018);
            case 3 -> {
                ctx.fill(0, 0, W, H, 0xFF000000);
                int sz = Math.min(W, H);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, (W - sz) / 2, (H - sz) / 2,
                        0f, 0f, sz, sz, 256, 256, 256, 256);
                ctx.fill(0, 0, W, H, 0xC0000000);
            }
            case 4 -> drawAurora(ctx, sec);
            case 5 -> drawStarfield(ctx, sec);
            case 6 -> drawMatrix(ctx, sec);
            case 7 -> drawEmbers(ctx, sec);
            case 8 -> drawOcean(ctx, sec);
            case 9 -> drawGrid(ctx, sec, acc);
            case 10 -> drawBubbles(ctx, sec);
            case 11 -> drawFireflies(ctx, sec);
            case 12 -> drawRain(ctx, sec);
            case 13 -> drawPlasma(ctx, sec);
            case 14 -> drawWarp(ctx, sec);
            case 15 -> drawSnowfall(ctx, sec);
            case 16 -> drawDisco(ctx, sec);
            case 17 -> drawRings(ctx, sec, acc);
            case 18 -> drawMeteors(ctx, sec);
            case 19 -> drawSunset(ctx, sec);
            default -> ctx.fill(0, 0, W, H, 0xFF101018);
        }
    }

    private void drawAurora(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF040816, 0xFF0A1C2E);
        for (int i = 0; i < 40; i++) {
            int x = (int) (hash(i) * W), y = (int) (hash(i + 77) * H * 0.6f);
            ctx.fill(x, y, x + 1, y + 1, 0x88FFFFFF);
        }
        for (int i = 0; i < 3; i++) {
            for (int x = 0; x < W; x += 4) {
                double wave = Math.sin(x / 60.0 + sec * 0.7 + i * 2.0) * 22 + Math.sin(x / 23.0 - sec * 1.3 + i) * 8;
                int y = (int) (H * (0.18 + 0.13 * i) + wave);
                int len = (int) (H * 0.30 + 16 * Math.sin(x / 40.0 + sec + i));
                double hue = 0.36 + 0.14 * i + 0.05 * Math.sin(sec * 0.5 + x / 200.0);
                ctx.fillGradient(x, y, x + 4, y + len, hsv(hue, 0.75, 1.0, 0x55), hsv(hue, 0.75, 1.0, 0x00));
            }
        }
    }

    private void drawStarfield(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF02030A, 0xFF0A1030);
        for (int i = 0; i < 130; i++) {
            double speed = 4 + hash(i * 3 + 2) * 16;
            int x = (int) (((hash(i * 3) * W - sec * speed) % W + W) % W);
            int y = (int) (hash(i * 3 + 1) * H);
            int sz = hash(i * 5 + 7) > 0.85f ? 2 : 1;
            int a = (int) (120 + 135 * (0.5 + 0.5 * Math.sin(sec * 2 + i)));
            ctx.fill(x, y, x + sz, y + sz, (a << 24) | 0xFFFFFF);
        }
    }

    private void drawMatrix(DrawContext ctx, double sec) {
        ctx.fill(0, 0, W, H, 0xFF020A04);
        int cols = W / 8;
        for (int c = 0; c < cols; c++) {
            double speed = 40 + hash(c) * 70;
            double head = (sec * speed + hash(c + 991) * (H + 160)) % (H + 160) - 40;
            for (int k = 0; k < 12; k++) {
                int y = (int) (head - k * 8);
                if (y < -8 || y > H) continue;
                int a = (int) (230 * (1 - k / 12.0));
                int col = k == 0 ? 0xCCFFCC : 0x2BFF5A;
                ctx.fill(c * 8, y, c * 8 + 5, y + 6, (a << 24) | col);
            }
        }
    }

    private void drawEmbers(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF1A0505, 0xFF541408);
        for (int i = 0; i < 90; i++) {
            double speed = 12 + hash(i * 3) * 34;
            double p = (sec * speed + hash(i * 3 + 1) * H) % H;
            int y = (int) (H - p);
            int x = (int) (hash(i * 3 + 2) * W + Math.sin(sec * 1.2 + i) * 10);
            int sz = 1 + (int) (hash(i + 500) * 3);
            int a = (int) (220 * (1 - p / H));
            int col = hash(i + 900) > 0.5f ? 0xFF8A1E : 0xFFC84A;
            ctx.fill(x, y, x + sz, y + sz, (a << 24) | col);
        }
    }

    private void drawOcean(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF03203A, 0xFF0A5C8A);
        int[] cols = {0x4430A0D8, 0x5520709E, 0x66103C66};
        for (int l = 0; l < 3; l++) {
            for (int x = 0; x < W; x += 3) {
                int y = (int) (H * (0.55 + 0.12 * l) + Math.sin(x / 38.0 + sec * (0.8 + 0.3 * l) + l * 1.7) * 7
                        + Math.sin(x / 17.0 - sec * 1.1) * 3);
                ctx.fill(x, y, x + 3, H, cols[l]);
            }
        }
    }

    private void drawGrid(DrawContext ctx, double sec, int acc) {
        ctx.fillGradient(0, 0, W, H, 0xFF0A0420, 0xFF2A0A4A);
        int hz = (int) (H * 0.55);
        int rad = (int) (H * 0.16);
        for (int dy = 0; dy < rad; dy++) {
            int dx = (int) Math.sqrt(rad * rad - dy * dy);
            ctx.fill(W / 2 - dx, hz - dy - 1, W / 2 + dx, hz - dy, hsv(0.95 - 0.12 * dy / (double) rad, 0.8, 1.0, 0xFF));
        }
        ctx.fill(0, hz, W, H, 0xFF0A0418);
        int gc = (acc & 0xFFFFFF) | 0xAA000000;
        double phase = (sec * 0.35) % 1.0;
        for (int r2 = 0; r2 < 12; r2++) {
            double f = (r2 + phase) / 12.0;
            int y = (int) (hz + (H - hz) * f * f);
            ctx.fill(0, y, W, y + 1, gc);
        }
        for (int k = -8; k <= 8; k++) {
            for (int y = hz; y < H; y += 6) {
                int x = (int) (W / 2.0 + k * (y - hz) * W / (double) (H - hz) / 9.0);
                ctx.fill(x, y, x + 1, y + 6, gc);
            }
        }
    }

    private void drawBubbles(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF04263B, 0xFF0C6B8F);
        for (int i = 0; i < 40; i++) {
            int rad = 2 + (int) (hash(i) * 6);
            double speed = 14 + hash(i + 50) * 30;
            double p = (sec * speed + hash(i + 100) * H) % (H + 20);
            int y = (int) (H + 10 - p);
            int x = (int) (hash(i + 200) * W + Math.sin(sec + i) * 8);
            disc(ctx, x, y, rad, 0x33FFFFFF);
            ctx.fill(x - rad / 2, y - rad / 2, x - rad / 2 + 1, y - rad / 2 + 1, 0x99FFFFFF);
        }
    }

    private void drawFireflies(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF04100A, 0xFF0C2A18);
        for (int i = 0; i < 45; i++) {
            int x = (int) (hash(i) * W + Math.sin(sec * 0.6 + i) * 20);
            int y = (int) (hash(i + 90) * H + Math.cos(sec * 0.5 + i * 1.7) * 15);
            double pulse = 0.5 + 0.5 * Math.sin(sec * 1.5 + i * 1.9);
            int a = (int) (255 * pulse);
            ctx.fill(x - 2, y - 2, x + 3, y + 3, ((a / 6) << 24) | 0xFFF59E);
            ctx.fill(x - 1, y - 1, x + 2, y + 2, ((a / 3) << 24) | 0xFFF59E);
            ctx.fill(x, y, x + 1, y + 1, (a << 24) | 0xFFFFD0);
        }
    }

    private void drawRain(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF0B0F1A, 0xFF1A2434);
        for (int i = 0; i < 140; i++) {
            double speed = 260 + hash(i) * 220;
            int y = (int) ((sec * speed + hash(i + 300) * H) % (H + 24)) - 12;
            int x = (int) (hash(i + 700) * W);
            ctx.fill(x, y, x + 1, y + 9, 0x66AFC8E8);
        }
        double fl = Math.sin(sec * 1.3) * Math.sin(sec * 7.1) - 0.85;
        if (fl > 0) ctx.fill(0, 0, W, H, (((int) Math.min(0x50, fl * 600)) << 24) | 0xFFFFFF);
    }

    private void drawPlasma(DrawContext ctx, double sec) {
        for (int x = 0; x < W; x += 16) {
            for (int y = 0; y < H; y += 16) {
                double v = Math.sin(x / 50.0 + sec) + Math.sin(y / 40.0 - sec * 0.8) + Math.sin((x + y) / 70.0 + sec * 0.5);
                ctx.fill(x, y, x + 16, y + 16, hsv(0.72 + v * 0.07, 0.8, 0.28 + 0.1 * (v + 3) / 6.0, 0xFF));
            }
        }
    }

    private void drawWarp(DrawContext ctx, double sec) {
        ctx.fill(0, 0, W, H, 0xFF02030A);
        double cx = W / 2.0, cy = H / 2.0, maxR = Math.hypot(cx, cy);
        for (int i = 0; i < 160; i++) {
            double ang = hash(i) * Math.PI * 2;
            double p = (sec * 0.45 + hash(i + 400)) % 1.0;
            double rad = p * p * maxR;
            int x = (int) (cx + Math.cos(ang) * rad);
            int y = (int) (cy + Math.sin(ang) * rad);
            int sz = p > 0.7 ? 2 : 1;
            int a = (int) (60 + 195 * p);
            ctx.fill(x, y, x + sz, y + sz, (a << 24) | 0xCFE0FF);
            double rad2 = Math.max(0, p - 0.04);
            rad2 = rad2 * rad2 * maxR;
            int xm = (int) (cx + Math.cos(ang) * (rad + rad2) / 2);
            int ym = (int) (cy + Math.sin(ang) * (rad + rad2) / 2);
            ctx.fill(xm, ym, xm + 1, ym + 1, ((a / 2) << 24) | 0xCFE0FF);
        }
    }

    private void drawSnowfall(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF1B2A44, 0xFF8FA6C8);
        for (int i = 0; i < 150; i++) {
            int sz = 1 + (int) (hash(i) * 3);
            double speed = 15 + sz * 14;
            int y = (int) ((sec * speed + hash(i + 60) * H) % H);
            int x = (int) (hash(i + 120) * W + Math.sin(sec * 0.8 + i) * 10);
            ctx.fill(x, y, x + sz, y + sz, 0xCCFFFFFF);
        }
    }

    private void drawDisco(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, hsv(sec * 0.05, 0.7, 0.30, 0xFF), hsv(sec * 0.05 + 0.35, 0.7, 0.18, 0xFF));
        for (int i = 0; i < 5; i++) {
            int x = (int) (W / 2.0 + Math.sin(sec * 0.8 + i * 1.3) * W * 0.4);
            double hue = sec * 0.1 + i * 0.2;
            ctx.fillGradient(x - 10, 0, x + 10, H, hsv(hue, 0.6, 1.0, 0x55), hsv(hue, 0.6, 1.0, 0x00));
        }
    }

    private void drawRings(DrawContext ctx, double sec, int acc) {
        ctx.fillGradient(0, 0, W, H, 0xFF08060F, 0xFF120A24);
        double cx = W / 2.0, cy = H / 2.0, maxR = Math.max(W, H) * 0.65;
        for (int i = 0; i < 5; i++) {
            double p = (sec * 0.25 + i / 5.0) % 1.0;
            double rad = p * maxR;
            int a = (int) (180 * (1 - p));
            for (int k = 0; k < 140; k++) {
                double ang = k * Math.PI * 2 / 140;
                int x = (int) (cx + Math.cos(ang) * rad);
                int y = (int) (cy + Math.sin(ang) * rad);
                ctx.fill(x, y, x + 2, y + 2, (a << 24) | (acc & 0xFFFFFF));
            }
        }
    }

    private void drawMeteors(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF03050E, 0xFF0B1230);
        for (int i = 0; i < 70; i++) {
            int x = (int) (hash(i) * W), y = (int) (hash(i + 140) * H);
            int a = (int) (60 + 120 * (0.5 + 0.5 * Math.sin(sec * 1.7 + i)));
            ctx.fill(x, y, x + 1, y + 1, (a << 24) | 0xFFFFFF);
        }
        for (int i = 0; i < 6; i++) {
            double period = 3.0 + hash(i) * 2.5;
            double p = ((sec + hash(i + 20) * period) % period) / period;
            if (p > 0.5) continue;
            double q = p / 0.5;
            double hx = hash(i + 30) * W * 0.9 + q * W * 0.35;
            double hy = hash(i + 40) * H * 0.4 + q * H * 0.45;
            for (int k = 0; k < 16; k++) {
                int x = (int) (hx - k * 3);
                int y = (int) (hy - k * 2.2);
                int a = (int) (230 * (1 - k / 16.0) * (1 - q * 0.5));
                ctx.fill(x, y, x + 2, y + 2, (a << 24) | 0xD8E8FF);
            }
        }
    }

    private void drawSunset(DrawContext ctx, double sec) {
        ctx.fillGradient(0, 0, W, H, 0xFF241044, 0xFFFF8A4D);
        int hz = (int) (H * 0.68);
        int rad = Math.max(16, (int) (H * 0.13));
        int sy = hz - (int) (Math.sin(sec * 0.25) * 6) - rad / 3;
        disc(ctx, W / 2, sy, rad, 0xFFFFD27A);
        for (int i = 0; i < 6; i++) {
            double speed = 4 + hash(i) * 8;
            int cx = (int) (((hash(i + 10) * W + sec * speed) % (W + 120)) - 60);
            int cy = (int) (H * (0.12 + 0.4 * hash(i + 20)));
            ctx.fill(cx, cy, cx + 60, cy + 6, 0x55FFC9A0);
            ctx.fill(cx + 10, cy - 5, cx + 44, cy, 0x55FFC9A0);
            ctx.fill(cx + 20, cy + 6, cx + 54, cy + 10, 0x33FFC9A0);
        }
        ctx.fill(0, hz, W, H, 0xFF1A0A2A);
        for (int k = 0; k < 6; k++) {
            ctx.fill(W / 2 - rad + k * 4, hz + 4 + k * 6, W / 2 + rad - k * 4, hz + 6 + k * 6, 0x66FFB46A);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = accent();
        long t = System.currentTimeMillis();

        rawMx = mouseX;
        rawMy = mouseY;
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) OX, (float) OY);
        mouseX -= OX;
        mouseY -= OY;
        ctx.fill(-3, -3, W + 3, H + 3, 0x55000000);
        drawBackground(ctx, t, acc);
        // soft vignette
        ctx.fillGradient(0, 0, W, 60, 0x66000000, 0x00000000);
        ctx.fillGradient(0, H - 70, W, H, 0x00000000, 0x77000000);

        // snow (classic backgrounds only)
        if (bgMode() <= 3) {
            for (int i = 0; i < 70; i++) {
                int x = (int) ((((i * 61) % 100) / 100f) * W + Math.sin(t / 900.0 + i) * 6);
                int y = (int) (((t / (20 + (i % 5) * 8)) + i * 53L) % Math.max(1, H));
                ctx.fill(x, y, x + 2, y + 2, 0xAAFFFFFF);
            }
        }

        // header
        ctx.fill(0, 0, W, 28, 0xDD0B0B14);
        ctx.fill(0, 28, W, 29, acc);
        ctx.fillGradient(0, 29, W, 33, (acc & 0x00FFFFFF) | 0x44000000, 0x00000000);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, 6, 4, 0f, 0f, 20, 20, 256, 256, 256, 256);
        ctx.drawText(textRenderer, Text.literal("XLetxzX Visuals"), 32, 6, acc, true);
        int online = OnlineCounter.getCount();
        if (OnlineCounter.configured() && Config.get().onlineCount && online >= 0) {
            ctx.fill(32, 19, 36, 23, 0xFF3DDC84);
            ctx.drawText(textRenderer, Text.literal(online + (online == 1 ? " player online" : " players online")),
                    40, 17, 0xFF9FE8BC, false);
        } else {
            ctx.drawText(textRenderer, Text.literal("v" + VERSION + " - 1.21.11"), 32, 17, 0xFF8888AA, false);
        }

        // bottom bar
        ctx.fill(0, H - 24, W, H, 0xDD0B0B14);
        ctx.fill(0, H - 24, W, H - 23, 0x33FFFFFF);
        int mw = madeWidth();
        box(ctx, 84, H - 20, mw, 16, 0xFFE8435A, 0xFFE8435A, true);
        double beat = Math.pow(Math.max(0.0, Math.sin(t / 230.0)), 8);
        float hs = (float) (1.0 + 0.45 * beat);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(92f, (float) (H - 12));
        ctx.getMatrices().scale(hs, hs);
        ctx.drawText(textRenderer, Text.literal("\u2665"), -3, -4, beat > 0.2 ? 0xFFFFFFFF : 0xFFFFC0C8, false);
        ctx.getMatrices().popMatrix();
        ctx.drawText(textRenderer, Text.literal(MADE), 102, H - 16, 0xFFFFFFFF, false);

        if (page == HOME) {
            int img = Math.min(H - 90, (W - 176) / 3);
            MinecraftClient mc = MinecraftClient.getInstance();
            String who = "XLetxzX";
            if (mc.player != null) {
                ctx.getMatrices().popMatrix();
                float emx = rawMx, emy = rawMy;
                if (Config.get().mode3d) {
                    emx = (float) (OX + 20 + img / 2.0 + Math.sin(t / 700.0) * 90);
                    emy = OY + 40 + img / 3f;
                }
                InventoryScreen.drawEntity(ctx, OX + 20, OY + 40, OX + 20 + img, OY + 40 + img,
                        (int) (img * 0.42f), 0.0625f, emx, emy, mc.player);
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate((float) OX, (float) OY);
                who = mc.player.getName().getString();
            } else {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, 20, 40, 0f, 0f, img, img, 256, 256, 256, 256);
            }
            ctx.fill(20, 40 + img + 6, 20 + img, 40 + img + 8, acc);
            drawCentered(ctx, who, 20 + img / 2, 40 + img + 14, 0xFFFFFFFF);

            int regionStart = 20 + img + 20;
            int regionEnd = W - 176 - 10;
            int cx = (regionStart + regionEnd) / 2;
            int gy = H < 300 ? 25 : 28;
            int by = Math.max(80, H / 2 - 10 - (H < 300 ? 28 : 0));
            panel(ctx, cx - 115, by - 62, 230, gy * 4 + 22 + 62 + 32, acc);
            drawCentered(ctx, "XLetxzX Visuals", cx, by - 50, acc);
            drawCentered(ctx, "v" + VERSION, cx, by - 38, 0xFF8888AA);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime xmas = LocalDateTime.of(now.getYear(), 12, 25, 0, 0);
            if (now.isAfter(xmas)) xmas = xmas.plusYears(1);
            Duration d = Duration.between(now, xmas);
            drawCentered(ctx, "Christmas in " + d.toDays() + "d " + d.toHoursPart() + "h " + d.toMinutesPart() + "m",
                    cx, by - 24, 0xFFFFD27A);
            drawCentered(ctx, "Open this menu with Right Shift", cx, by + gy * 4 + 28, 0xFF777788);
        } else if (page == DESIGN) {
            ctx.drawText(textRenderer, Text.literal("Customize the look"), 90, 40, 0xFFFFFFFF, false);
            drawSection(ctx, "LOOK", secY[0]);
            drawSection(ctx, "THEMES", secY[1]);
            drawSection(ctx, "BACKGROUND", secY[2], W - 110);
            String pg = (bgPage + 1) + "/" + ((BG_NAMES.length + 11) / 12);
            ctx.drawText(textRenderer, Text.literal(pg), W - 64 - textRenderer.getWidth(pg), secY[2], 0xFF8888AA, false);
        } else if (page == SETTINGS) {
            ctx.drawText(textRenderer, Text.literal("Settings"), 90, 40, 0xFFFFFFFF, false);
            ctx.drawText(textRenderer, Text.literal("INTERFACE"), 230, 62, 0xFF8888AA, false);
            drawSection(ctx, "GENERAL", 62);
            drawSection(ctx, "DISCORD", 168);
            ctx.drawText(textRenderer, Text.literal("Shows XLetxzX Visuals as your Discord activity."), 20, 208, 0xFF8888AA, false);
            drawSection(ctx, "PLAYERS ONLINE", 218);
            ctx.drawText(textRenderer, Text.literal("Sends an anonymous random ID once a minute to count players."),
                    20, 258, 0xFF8888AA, false);
            ctx.drawText(textRenderer, Text.literal("Turn it off to opt out. No name, no IP is stored by the mod."),
                    20, 270, 0xFF8888AA, false);
            if (!OnlineCounter.configured()) {
                ctx.drawText(textRenderer, Text.literal("Counter server not set up yet."), 20, 286, 0xFFFFD27A, false);
            }
        } else if (page == CREDITS) {
            ctx.drawText(textRenderer, Text.literal("Credits"), 20, 44, acc, true);
            String[] lines = {
                "XLetxzX Visuals - made by XLetxzX",
                "",
                "Included mods belong to their respective developers:",
                "Sodium, Lithium, FerriteCore, ImmediatelyFast, Entity Culling,",
                "Mod Menu, Cloth Config, Simple Voice Chat, Litematica and many more.",
                "Thanks to everyone who builds these mods."
            };
            for (int i = 0; i < lines.length; i++) {
                ctx.drawText(textRenderer, Text.literal(lines[i]), 20, 66 + i * 12, 0xFFCCCCCC, false);
            }
        } else if (page == IMPORT) {
            int areaW = W - 40;
            ctx.drawText(textRenderer, Text.literal("Mods - import & folder"), 90, 40, 0xFFFFFFFF, false);
            // drop zone
            int zy = 64, zh = 84;
            ctx.fill(20, zy, 20 + areaW, zy + zh, 0x66101420);
            for (int x = 20; x < 20 + areaW; x += 10) {
                ctx.fill(x, zy, Math.min(x + 5, 20 + areaW), zy + 1, acc);
                ctx.fill(x, zy + zh - 1, Math.min(x + 5, 20 + areaW), zy + zh, acc);
            }
            for (int y = zy; y < zy + zh; y += 10) {
                ctx.fill(20, y, 21, Math.min(y + 5, zy + zh), acc);
                ctx.fill(20 + areaW - 1, y, 20 + areaW, Math.min(y + 5, zy + zh), acc);
            }
            drawCentered(ctx, "Drop a mod (.jar) here", W / 2, zy + 26, 0xFFFFFFFF);
            drawCentered(ctx, "Drag the file from your computer into this window", W / 2, zy + 42, 0xFF8888AA);
            drawCentered(ctx, "Yours or from a friend - it is copied into your mods folder", W / 2, zy + 54, 0xFF8888AA);
            drawCentered(ctx, "Click a file below (or the button) to open the mods folder", W / 2, zy + 66, 0xFF8888AA);
            // link
            drawSection(ctx, "OR PASTE A DIRECT DOWNLOAD LINK", 164);
            ctx.drawText(textRenderer, Text.literal("Only import mods you trust. Restart Minecraft afterwards."),
                    20, 214, 0xFFFFD27A, false);
            drawSection(ctx, "IN YOUR MODS FOLDER (" + modFiles.size() + ")", 236);
            int max = Math.max(1, (H - 24 - 252) / 11);
            for (int i = 0; i < Math.min(max, modFiles.size()); i++) {
                boolean fresh = !startupMods.contains(modFiles.get(i));
                ctx.drawText(textRenderer, Text.literal("\u2022 " + modFiles.get(i) + (fresh ? "  (new - restart to load)" : "")),
                        24, 252 + i * 11, fresh ? 0xFFFFD27A : 0xFFBBBBCC, false);
            }
            if (modFiles.size() > max) {
                ctx.drawText(textRenderer, Text.literal("... and " + (modFiles.size() - max) + " more"),
                        24, 252 + max * 11, 0xFF777788, false);
            }
        } else {
            // EXTRAS
            for (int[] h : headers) {
                String label = CAT_NAMES[h[1]];
                ctx.fill(20, h[0] + 3, 24, h[0] + 11, CAT_COLORS[h[1]]);
                ctx.drawText(textRenderer, Text.literal(label), 30, h[0] + 3, 0xFF9999BB, false);
                ctx.fill(30 + textRenderer.getWidth(label) + 8, h[0] + 7, W - 20, h[0] + 8, 0x22FFFFFF);
            }
            boolean anyCard = false;
            for (Btn bb : btns) if (bb.kind == 7) { anyCard = true; break; }
            if (!anyCard) {
                drawCentered(ctx, "No mods found", W / 2, H / 2, 0xFF888899);
            }
            if (maxScroll > 0) {
                int trackH = listBottom - listTop;
                int barH = Math.max(20, trackH * trackH / (trackH + maxScroll));
                int barY = listTop + (int) ((trackH - barH) * (scroll / (float) maxScroll));
                ctx.fill(W - 10, listTop, W - 8, listBottom, 0x22FFFFFF);
                ctx.fill(W - 10, barY, W - 8, barY + barH, acc);
            }
        }

        for (Btn b : btns) drawBtn(ctx, b, mouseX, mouseY);
        ctx.getMatrices().popMatrix();
        if (searchField != null) searchField.render(ctx, (int) rawMx, (int) rawMy, delta);
        if (linkField != null) linkField.render(ctx, (int) rawMx, (int) rawMy, delta);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) OX, (float) OY);

        if (page == HOME) {
            String[] log = {
                "Changelog", "v" + VERSION, "+ Window mode", "+ 30 backgrounds",
                "+ MODS tab & folder", "+ Tutorial, 3D mode", "+ Animated heart", "+ FPS & Visuals only"
            };
            int bx = W - 168, by = 36, bw = 160;
            panel(ctx, bx, by, bw, 12 + log.length * 12, acc);
            for (int i = 0; i < log.length; i++) {
                boolean head = i == 0;
                boolean ver = log[i].startsWith("v");
                int col = head ? acc : (ver ? 0xFFFFFFFF : 0xFFBBBBBB);
                ctx.drawText(textRenderer, Text.literal(log[i]), bx + 6, by + 8 + i * 12, col, head);
            }
            // pencil icon
            int px = bx + bw - 16, py = by + 6;
            for (int i = 0; i < 7; i++) ctx.fill(px + i, py + 6 - i, px + i + 2, py + 7 - i, 0xFFFFFFFF);
            ctx.fill(px - 1, py + 7, px + 2, py + 8, 0xFFFFC857);
            if (Config.get().tutorial) {
                String[] tut = {"Tutorial", "Right Shift: menu", "EXTRAS: mod settings", "MODS: drop .jar here", "Gear: turn tutorial off"};
                int ty = by + 12 + log.length * 12 + 8;
                panel(ctx, bx, ty, bw, 12 + tut.length * 12, acc);
                for (int i = 0; i < tut.length; i++) {
                    ctx.drawText(textRenderer, Text.literal(tut[i]), bx + 6, ty + 8 + i * 12, i == 0 ? acc : 0xFFBBBBBB, i == 0);
                }
            }
        }

        if (System.currentTimeMillis() < noteUntil) {
            int nw = textRenderer.getWidth(note) + 16;
            ctx.fill(W / 2 - nw / 2, H - 46, W / 2 + nw / 2, H - 30, 0xEE0B0B14);
            ctx.fill(W / 2 - nw / 2, H - 46, W / 2 - nw / 2 + 2, H - 30, acc);
            drawCentered(ctx, note, W / 2, H - 42, 0xFFFFD27A);
        }
        ctx.fill(0, 0, W, 1, acc);
        ctx.fill(0, H - 1, W, H, acc);
        ctx.fill(0, 0, 1, H, acc);
        ctx.fill(W - 1, 0, W, H, acc);
        ctx.getMatrices().popMatrix();
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }
}
