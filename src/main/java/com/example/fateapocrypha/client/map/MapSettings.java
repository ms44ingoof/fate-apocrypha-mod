package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.story.StoryLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.loading.FMLPaths;

/** Настройки карты (цвет контура и рамки, слои) и открытые игроком места. Хранятся в config/fateapocrypha-map.properties. */
public final class MapSettings {
    /** Нежная палитра для контура и рамки. */
    public static final int[] PALETTE = {
            0xE8A0BF, 0xF5B193, 0xF0D58A, 0x9DD9B3, 0x8CD3D8,
            0x8FB8F0, 0xA9A8EE, 0xB9A6EC, 0xDDA8E6, 0xB99A8E
    };

    public static int outline = 0xE8A0BF;
    public static int frame = 0xB9A6EC;
    public static boolean contours = true;
    public static boolean labels = true;
    public static boolean roads = true;
    public static boolean grid = false;

    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;
    private static final Set<String> SEEN = new HashSet<>();
    private static String seenKey = null;

    private MapSettings() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fateapocrypha-map.properties");
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream in = Files.newInputStream(path)) {
            PROPS.load(in);
            outline = parseColor(PROPS.getProperty("outline"), outline);
            frame = parseColor(PROPS.getProperty("frame"), frame);
            contours = Boolean.parseBoolean(PROPS.getProperty("contours", "true"));
            labels = Boolean.parseBoolean(PROPS.getProperty("labels", "true"));
            roads = Boolean.parseBoolean(PROPS.getProperty("roads", "true"));
            grid = Boolean.parseBoolean(PROPS.getProperty("grid", "false"));
        } catch (IOException | RuntimeException ignored) {
            // повреждённый файл: остаются значения по умолчанию
        }
    }

    private static int parseColor(String text, int fallback) {
        if (text == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(text.replace("#", ""), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static void save() {
        PROPS.setProperty("outline", String.format("%06X", outline));
        PROPS.setProperty("frame", String.format("%06X", frame));
        PROPS.setProperty("contours", Boolean.toString(contours));
        PROPS.setProperty("labels", Boolean.toString(labels));
        PROPS.setProperty("roads", Boolean.toString(roads));
        PROPS.setProperty("grid", Boolean.toString(grid));
        try (OutputStream out = Files.newOutputStream(file())) {
            PROPS.store(out, "Fate/Apocrypha world map");
        } catch (IOException ignored) {
            // карту это не ломает
        }
    }

    // ---------- открытые места ----------
    private static String worldKey(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData data = mc.getCurrentServer();
        return data != null ? "mp:" + data.ip : "unknown";
    }

    private static void loadSeen(Minecraft mc) {
        load();
        String key = worldKey(mc);
        if (key.equals(seenKey)) {
            return;
        }
        seenKey = key;
        SEEN.clear();
        String stored = PROPS.getProperty("seen." + key, "");
        for (String id : stored.split(",")) {
            if (!id.isBlank()) {
                SEEN.add(id.trim());
            }
        }
    }

    public static boolean isSeen(Minecraft mc, StoryLocation location) {
        loadSeen(mc);
        return SEEN.contains(location.primary().name());
    }

    /** Раз в секунду: место открывается, когда игрок оказался рядом. */
    public static void tickDiscovery(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        loadSeen(mc);
        boolean changed = false;
        for (StoryLocation location : StoryLocation.values()) {
            if (location.primary() == location && !SEEN.contains(location.name())
                    && location.distanceTo(mc.player.getX(), mc.player.getZ()) <= location.radius() + 70.0D) {
                SEEN.add(location.name());
                changed = true;
            }
        }
        if (changed) {
            PROPS.setProperty("seen." + seenKey, String.join(",", SEEN));
            save();
        }
    }
}
