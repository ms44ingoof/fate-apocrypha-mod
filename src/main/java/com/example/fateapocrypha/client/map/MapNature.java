package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.world.CastleSite;
import com.example.fateapocrypha.world.TrifasRoads;
import com.example.fateapocrypha.world.TrifasTerrain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Природа на карте: деревья по лесам (лиственные, берёзы, тёмный лес, сакуры, ели и заснеженные ели, акации),
 * редкие деревья на равнинах и горные пики со снежными шапками. Места считаются один раз в фоне из тех же функций
 * рельефа и биомов, что и сам мир, поэтому леса на карте совпадают с настоящими. Чем крупнее масштаб, тем больше
 * деревьев показывается (у каждого дерева свой уровень детализации).
 */
final class MapNature {
    /** Вид: 0 лиственное, 1 берёза, 2 тёмный лес, 3 сакура, 4 ель, 5 заснеженная ель, 6 акация, 7 скала, 8 снежный пик. */
    record Tree(float x, float z, byte kind, byte lod, byte size) {
    }

    private static final int GRID = 14;
    private static final int TILE = 160;
    private static volatile Map<Long, List<Tree>> tiles = null;
    private static volatile boolean started = false;

    private MapNature() {
    }

    private static double hash01(int x, int z, int salt) {
        long h = x * 374761393L + z * 668265263L + salt * 1442695040888963407L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= (h >>> 16);
        return (h & 0xFFFFL) / 65535.0D;
    }

    private static long key(int tx, int tz) {
        return ((long) tx << 32) ^ (tz & 0xFFFFFFFFL);
    }

    private static void ensure() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(() -> {
            try {
                compute();
            } catch (RuntimeException e) {
                started = false;
            }
        }, "fate-map-nature");
        t.setDaemon(true);
        t.start();
    }

    private static void compute() {
        Map<Long, List<Tree>> out = new HashMap<>();
        int x0 = (int) MapTerrain.X0;
        int z0 = (int) MapTerrain.Z0;
        int x1 = (int) (MapTerrain.X0 + MapTerrain.WIDTH);
        int z1 = (int) (MapTerrain.Z0 + MapTerrain.HEIGHT);
        for (int gx = x0; gx < x1; gx += GRID) {
            for (int gz = z0; gz < z1; gz += GRID) {
                int x = gx + (int) (hash01(gx, gz, 1) * GRID);
                int z = gz + (int) (hash01(gx, gz, 2) * GRID);
                int h = TrifasTerrain.height(x, z);
                if (h <= TrifasTerrain.SEA + 2 || TrifasTerrain.padDist(x, z) < 55.0D || TrifasRoads.roadDist(x, z) < 7.0D
                        || (x > CastleSite.X0 - 20 && x < CastleSite.X0 + CastleSite.W + 20
                        && z > CastleSite.Z0 - 20 && z < CastleSite.Z0 + CastleSite.D + 60)) {
                    continue;
                }
                String biome = TrifasTerrain.biome(x, z, h);
                int kind = -1;
                double density = 0.0D;
                switch (biome) {
                    case "minecraft:forest" -> { kind = 0; density = 0.85D; }
                    case "minecraft:flower_forest" -> { kind = 0; density = 0.55D; }
                    case "minecraft:birch_forest" -> { kind = 1; density = 0.8D; }
                    case "minecraft:dark_forest" -> { kind = 2; density = 1.0D; }
                    case "minecraft:cherry_grove" -> { kind = 3; density = 0.6D; }
                    case "minecraft:taiga" -> { kind = 4; density = 0.9D; }
                    case "minecraft:snowy_taiga" -> { kind = 5; density = 0.7D; }
                    case "minecraft:grove" -> { kind = 5; density = 0.45D; }
                    case "minecraft:savanna" -> { kind = 6; density = 0.12D; }
                    case "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow" -> { kind = 0; density = 0.07D; }
                    case "minecraft:windswept_hills" -> { kind = 4; density = 0.25D; }
                    case "minecraft:jagged_peaks", "minecraft:stony_peaks" -> { kind = 7; density = 0.22D; }
                    case "minecraft:frozen_peaks" -> { kind = 8; density = 0.25D; }
                    default -> { }
                }
                if (kind < 0 || hash01(gx, gz, 3) > density) {
                    continue;
                }
                if (kind >= 7 && h >= 172 && hash01(gx, gz, 5) < 0.5D) {
                    kind = 8;   // самые высокие пики со снегом
                }
                byte lod = (byte) (hash01(gx, gz, 4) * 3.0D);
                byte size = (byte) (hash01(gx, gz, 6) * 3.0D);
                out.computeIfAbsent(key(Math.floorDiv(x, TILE), Math.floorDiv(z, TILE)), k -> new ArrayList<>())
                        .add(new Tree(x, z, (byte) kind, lod, size));
            }
        }
        // дальние деревья рисуются раньше ближних
        for (List<Tree> list : out.values()) {
            list.sort((a, b) -> Float.compare(a.z(), b.z()));
        }
        tiles = out;
    }

    static void draw(GuiGraphics g, float zoom, int vx1, int vy1, int vx2, int vy2,
                     DoubleUnaryOperator sx, DoubleUnaryOperator sy) {
        ensure();
        Map<Long, List<Tree>> map = tiles;
        if (map == null || zoom < 0.24F) {
            return;
        }
        int maxLod = zoom < 0.4F ? 0 : zoom < 0.65F ? 1 : 2;
        // мировые границы видимой области (по угловым точкам; экранные функции линейные)
        double wx0 = invert(sx, vx1);
        double wx1 = invert(sx, vx2);
        double wz0 = invert(sy, vy1);
        double wz1 = invert(sy, vy2);
        int tx0 = Math.floorDiv((int) Math.floor(wx0) - 8, TILE);
        int tx1 = Math.floorDiv((int) Math.ceil(wx1) + 8, TILE);
        int tz0 = Math.floorDiv((int) Math.floor(wz0) - 8, TILE);
        int tz1 = Math.floorDiv((int) Math.ceil(wz1) + 30, TILE);
        for (int tz = tz0; tz <= tz1; tz++) {
            for (int tx = tx0; tx <= tx1; tx++) {
                List<Tree> list = map.get(key(tx, tz));
                if (list == null) {
                    continue;
                }
                for (Tree t : list) {
                    if (t.lod() > maxLod && t.kind() < 7) {
                        continue;
                    }
                    int x = (int) Math.round(sx.applyAsDouble(t.x()));
                    int y = (int) Math.round(sy.applyAsDouble(t.z()));
                    if (x < vx1 - 10 || x > vx2 + 10 || y < vy1 - 10 || y > vy2 + 16) {
                        continue;
                    }
                    drawTree(g, t, x, y, zoom);
                }
            }
        }
    }

    private static double invert(DoubleUnaryOperator f, double screen) {
        double a = f.applyAsDouble(0.0D);
        double b = f.applyAsDouble(1000.0D);
        return (screen - a) / ((b - a) / 1000.0D);
    }

    private static void drawTree(GuiGraphics g, Tree t, int x, int y, float zoom) {
        float s = Math.max(1.0F, Math.min(4.2F, zoom * (2.6F + t.size() * 0.5F)));
        int r = Math.max(1, Math.round(s));
        int trunk = Math.max(1, r / 2);
        switch (t.kind()) {
            case 0, 1, 2, 3, 6 -> {
                int dark = switch (t.kind()) {
                    case 1 -> 0x6FA95D;
                    case 2 -> 0x2B6137;
                    case 3 -> 0xE79AB8;
                    case 6 -> 0x8FA84A;
                    default -> 0x3F8E4B;
                };
                int light = MapBuildings.shade(dark, 1.22F);
                g.fill(x, y - 1, x + trunk, y + r, 0xFF000000 | (t.kind() == 1 ? 0xE8E2D4 : 0x6B4B2E));
                MapBuildings.disc(g, x, y - r, r, 0xFF000000 | dark);
                if (r >= 2) {
                    MapBuildings.disc(g, x - r / 3, y - r - r / 3, Math.max(1, r * 2 / 3), 0xFF000000 | light);
                }
            }
            case 4, 5 -> {
                int dark = t.kind() == 5 ? 0x5B8F78 : 0x2F7352;
                int light = t.kind() == 5 ? 0xEAF3F5 : 0x4A9A6C;
                int h = r * 3;
                g.fill(x, y - 1, x + trunk, y + 1, 0xFF5B4128);
                MapBuildings.poly(g, new int[] {x - r, x + r, x}, new int[] {y - r, y - r, y - h}, 0xFF000000 | dark);
                MapBuildings.poly(g, new int[] {x - r, x, x}, new int[] {y - r, y - r, y - h}, 0xFF000000 | light);
                if (r >= 2) {
                    MapBuildings.poly(g, new int[] {x - r + 1, x + r - 1, x}, new int[] {y - h / 2, y - h / 2, y - h - 1},
                            0xFF000000 | MapBuildings.shade(dark, 0.9F));
                }
            }
            default -> {   // горные пики: тёмная и светлая грани, снежная шапка
                int w = Math.max(3, Math.round(zoom * (11.0F + t.size() * 3.0F)));
                int h = Math.round(w * 1.1F);
                int rock = 0x8E8C9A;
                MapBuildings.poly(g, new int[] {x - w, x + w, x}, new int[] {y, y, y - h}, 0xFF000000 | rock);
                MapBuildings.poly(g, new int[] {x - w, x, x}, new int[] {y, y, y - h}, 0xFF000000 | MapBuildings.shade(rock, 1.18F));
                if (t.kind() == 8 && w >= 4) {
                    int cap = h * 2 / 5;
                    MapBuildings.poly(g, new int[] {x - w * 2 / 5, x + w * 2 / 5, x}, new int[] {y - h + cap, y - h + cap, y - h}, 0xFFF4F7FB);
                }
            }
        }
    }
}
