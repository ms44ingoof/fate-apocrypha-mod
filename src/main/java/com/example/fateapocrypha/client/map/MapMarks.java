package com.example.fateapocrypha.client.map;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Свои метки игрока на карте: флажок, сердце, звезда, дом или самоцвет, любой пастельный цвет и название. */
public final class MapMarks {
    public static final class Mark {
        public double x;
        public double z;
        public int color;
        public int icon;
        public String name;

        public Mark(double x, double z, int color, int icon, String name) {
            this.x = x;
            this.z = z;
            this.color = color;
            this.icon = icon;
            this.name = name;
        }
    }

    /** Пиксельные значки 9 в ширину. */
    public static final String[][] ICONS = {
            {"#........", "###......", "#####....", "#######..", "#####....", "###......", "#........", "#........", "#........"},
            {".##...##.", "####.####", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."},
            {"....#....", "....#....", "...###...", "#########", ".#######.", "..#####..", "..##.##..", ".##...##.", ".#.....#."},
            {"....#....", "...###...", "..#####..", ".#######.", "#########", ".##...##.", ".##.#.##.", ".##.#.##.", ".#######."},
            {"..#####..", ".#######.", "#########", ".#######.", "..#####..", "...###...", "....#...."}
    };

    private static final List<Mark> MARKS = new ArrayList<>();
    private static String loadedKey = null;

    private MapMarks() {
    }

    public static List<Mark> list(Minecraft mc) {
        String key = MapSettings.worldKey(mc);
        if (!key.equals(loadedKey)) {
            loadedKey = key;
            MARKS.clear();
            String stored = MapSettings.prop("marks." + key, "");
            for (String line : stored.split(";")) {
                String[] f = line.split("\\|", -1);
                if (f.length < 5) {
                    continue;
                }
                try {
                    String name = URLDecoder.decode(f[4], StandardCharsets.UTF_8);
                    MARKS.add(new Mark(Double.parseDouble(f[0]), Double.parseDouble(f[1]),
                            Integer.parseInt(f[2]) & 0xFFFFFF, Integer.parseInt(f[3]), name));
                } catch (RuntimeException ignored) {
                    // битая запись пропускается
                }
            }
        }
        return MARKS;
    }

    public static void save(Minecraft mc) {
        String key = MapSettings.worldKey(mc);
        StringBuilder sb = new StringBuilder();
        for (Mark m : MARKS) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(m.x).append('|').append(m.z).append('|').append(m.color).append('|').append(m.icon).append('|')
                    .append(URLEncoder.encode(m.name, StandardCharsets.UTF_8));
        }
        MapSettings.putProp("marks." + key, sb.toString());
        MapSettings.save();
    }

    /** Рисует значок с белой обводкой; (cx, cy) центр. */
    public static void drawIcon(GuiGraphics g, int icon, int cx, int cy, int rgb) {
        String[] rows = ICONS[Math.floorMod(icon, ICONS.length)];
        int x0 = cx - 4;
        int y0 = cy - rows.length / 2;
        paint(g, rows, x0 - 1, y0, 0xFFFFFFFF);
        paint(g, rows, x0 + 1, y0, 0xFFFFFFFF);
        paint(g, rows, x0, y0 - 1, 0xFFFFFFFF);
        paint(g, rows, x0, y0 + 1, 0xFFFFFFFF);
        paint(g, rows, x0, y0, 0xFF000000 | rgb);
    }

    private static void paint(GuiGraphics g, String[] rows, int x0, int y0, int argb) {
        for (int j = 0; j < rows.length; j++) {
            String r = rows[j];
            int i = 0;
            while (i < r.length()) {
                if (r.charAt(i) == '#') {
                    int s = i;
                    while (i < r.length() && r.charAt(i) == '#') {
                        i++;
                    }
                    g.fill(x0 + s, y0 + j, x0 + i, y0 + j + 1, argb);
                } else {
                    i++;
                }
            }
        }
    }
}
