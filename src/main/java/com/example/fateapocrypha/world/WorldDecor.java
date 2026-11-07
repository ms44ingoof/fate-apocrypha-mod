package com.example.fateapocrypha.world;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Случайные постройки открытого мира: дома, колодцы, лагеря, дозорные башни, святилища, руины и фонари вдоль дорог.
 * Постройки ставятся рядом с игроком, когда нужные чанки уже загружены, и запоминаются (каждая только один раз).
 * Мир разбит на клетки 72x72; в каждой клетке с вероятностью 55% стоит одна постройка. Возле дорог чаще дома и колодцы.
 */
public final class WorldDecor {
    private static final int CELL = 72;
    private static final String DATA = "fateapocrypha_decor";

    private WorldDecor() {
    }

    // ---------- хранилище построенных клеток ----------
    public static final class Done extends SavedData {
        private final Set<Long> keys = new HashSet<>();

        public static Done load(CompoundTag tag) {
            Done d = new Done();
            for (long k : tag.getLongArray("keys")) {
                d.keys.add(k);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            long[] arr = new long[keys.size()];
            int i = 0;
            for (long k : keys) {
                arr[i++] = k;
            }
            tag.putLongArray("keys", arr);
            return tag;
        }

        boolean has(long key) {
            return keys.contains(key);
        }

        void add(long key) {
            keys.add(key);
            setDirty();
        }
    }

    private static boolean loaded(ServerLevel level, int x, int z, int radius) {
        for (int dx : new int[] {-radius, radius}) {
            for (int dz : new int[] {-radius, radius}) {
                if (!level.hasChunk((x + dx) >> 4, (z + dz) >> 4)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Вызывается раз в пару секунд для игрока в мире Трифаса: строит не более одной постройки за вызов. */
    public static void tick(ServerLevel level, ServerPlayer player) {
        Done done = level.getDataStorage().computeIfAbsent(Done::load, Done::new, DATA);
        int px = (int) Math.floor(player.getX());
        int pz = (int) Math.floor(player.getZ());

        // фонари вдоль дорог
        List<double[]> lamps = TrifasRoads.lamps();
        for (int i = 0; i < lamps.size(); i++) {
            double[] lamp = lamps.get(i);
            if (Math.abs(lamp[0] - px) > 70.0D || Math.abs(lamp[1] - pz) > 70.0D) {
                continue;
            }
            long key = (1L << 62) | i;
            if (done.has(key)) {
                continue;
            }
            int x = (int) Math.round(lamp[0] + lamp[2] * 3.0D);
            int z = (int) Math.round(lamp[1] + lamp[3] * 3.0D);
            if (!loaded(level, x, z, 2)) {
                continue;
            }
            done.add(key);
            int y = BuildKit.ground(x, z);
            if (y >= TrifasTerrain.SEA + 1 && TrifasTerrain.padDist(x, z) > 30.0D) {
                BuildKit kit = new BuildKit(level);
                kit.fill(x, y + 1, z, x, y + 3, z, BuildKit.s(Blocks.OAK_FENCE));
                kit.set(x, y + 4, z, BuildKit.s(Blocks.LANTERN));
            }
        }

        // клетки с постройками вокруг игрока
        int ci = Math.floorDiv(px, CELL);
        int cj = Math.floorDiv(pz, CELL);
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                long key = ((long) i << 32) ^ (j & 0xFFFFFFFFL);
                if (done.has(key)) {
                    continue;
                }
                Random rnd = new Random(key * 0x9E3779B97F4A7C15L + 12345L);
                if (rnd.nextInt(100) >= 55) {
                    done.add(key);
                    continue;
                }
                int ax = i * CELL + 12 + rnd.nextInt(CELL - 24);
                int az = j * CELL + 12 + rnd.nextInt(CELL - 24);
                if (!loaded(level, ax, az, 7)) {
                    continue;
                }
                done.add(key);
                place(level, rnd, ax, az);
                return;
            }
        }
    }

    // ---------- выбор и подготовка места ----------
    private static void place(ServerLevel level, Random rnd, int ax, int az) {
        if (TrifasTerrain.padDist(ax, az) < 75.0D || TrifasRoads.roadDist(ax, az) < 9.0D) {
            return;
        }
        int sum = 0;
        int min = 1000;
        int max = -1000;
        int n = 0;
        for (int dx = -5; dx <= 5; dx += 5) {
            for (int dz = -5; dz <= 5; dz += 5) {
                int h = BuildKit.ground(ax + dx, az + dz);
                sum += h;
                min = Math.min(min, h);
                max = Math.max(max, h);
                n++;
            }
        }
        int y0 = Math.round(sum / (float) n);
        if (min < TrifasTerrain.SEA + 3 || max > 140 || max - min > 5) {
            return;   // вода, горы или слишком неровно
        }
        boolean nearRoad = TrifasRoads.roadDist(ax, az) < 70.0D;
        int roll = rnd.nextInt(10);
        int type;
        if (nearRoad) {
            type = roll < 3 ? 0 : roll < 6 ? 1 : roll < 8 ? 2 : 3;   // дом, колодец, лагерь, святилище
        } else {
            type = roll < 2 ? 2 : roll < 4 ? 3 : roll < 6 ? 4 : roll < 8 ? 5 : roll < 9 ? 0 : 1;   // + руины, башня
        }

        BuildKit kit = new BuildKit(level);
        prepare(kit, ax, az, 6, y0);
        switch (type) {
            case 0 -> cabin(kit, rnd, ax, az, y0);
            case 1 -> well(kit, ax, az, y0);
            case 2 -> camp(kit, rnd, ax, az, y0);
            case 3 -> shrine(kit, ax, az, y0);
            case 4 -> ruin(kit, rnd, ax, az, y0);
            default -> tower(kit, ax, az, y0);
        }
    }

    /** Выравнивает площадку: срезает выступы и деревья, подсыпает землю до уровня y0. */
    private static void prepare(BuildKit kit, int ax, int az, int r, int y0) {
        for (int x = ax - r; x <= ax + r; x++) {
            for (int z = az - r; z <= az + r; z++) {
                int g = BuildKit.ground(x, z);
                for (int y = y0 + 1; y <= Math.max(g, y0) + 14; y++) {
                    if (!kit.isAir(x, y, z)) {
                        kit.set(x, y, z, BuildKit.AIR);
                    }
                }
                for (int y = g; y < y0; y++) {
                    kit.set(x, y + 1, z, BuildKit.s(Blocks.DIRT));
                }
                kit.set(x, y0, z, BuildKit.s(Blocks.GRASS_BLOCK));
            }
        }
    }

    // ---------- постройки ----------
    private static void well(BuildKit kit, int cx, int cz, int y0) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                kit.set(cx + dx, y0, cz + dz, ring ? BuildKit.s(Blocks.GRAVEL) : BuildKit.s(Blocks.COBBLESTONE));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean center = dx == 0 && dz == 0;
                kit.set(cx + dx, y0, cz + dz, center ? BuildKit.s(Blocks.WATER) : BuildKit.s(Blocks.COBBLESTONE));
                kit.set(cx + dx, y0 - 1, cz + dz, center ? BuildKit.s(Blocks.WATER) : BuildKit.s(Blocks.COBBLESTONE));
                if (!center) {
                    kit.set(cx + dx, y0 + 1, cz + dz, BuildKit.s(Blocks.MOSSY_COBBLESTONE));
                }
            }
        }
        kit.set(cx, y0 - 2, cz, BuildKit.s(Blocks.COBBLESTONE));
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            kit.fill(cx + c[0], y0 + 2, cz + c[1], cx + c[0], y0 + 3, cz + c[1], BuildKit.s(Blocks.OAK_FENCE));
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    kit.set(cx + dx, y0 + 4, cz + dz, BuildKit.s(Blocks.OAK_SLAB));
                }
            }
        }
        kit.set(cx, y0 + 3, cz, BuildKit.s(Blocks.CHAIN));
        kit.set(cx, y0 + 2, cz, BuildKit.s(Blocks.LANTERN).setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    private static void cabin(BuildKit kit, Random rnd, int cx, int cz, int y0) {
        Block log = rnd.nextBoolean() ? Blocks.OAK_LOG : Blocks.SPRUCE_LOG;
        Block planks = log == Blocks.OAK_LOG ? Blocks.OAK_PLANKS : Blocks.SPRUCE_PLANKS;
        Block stairs = log == Blocks.OAK_LOG ? Blocks.OAK_STAIRS : Blocks.SPRUCE_STAIRS;
        Block slab = log == Blocks.OAK_LOG ? Blocks.OAK_SLAB : Blocks.SPRUCE_SLAB;
        Block door = log == Blocks.OAK_LOG ? Blocks.OAK_DOOR : Blocks.SPRUCE_DOOR;
        int x1 = cx - 3;
        int x2 = cx + 3;
        int z1 = cz - 2;
        int z2 = cz + 2;
        // фундамент и пол
        kit.fill(x1, y0, z1, x2, y0, z2, BuildKit.s(Blocks.COBBLESTONE));
        kit.fill(x1 + 1, y0, z1 + 1, x2 - 1, y0, z2 - 1, BuildKit.s(planks));
        // стены
        for (int y = y0 + 1; y <= y0 + 3; y++) {
            for (int x = x1; x <= x2; x++) {
                kit.set(x, y, z1, BuildKit.s(planks));
                kit.set(x, y, z2, BuildKit.s(planks));
            }
            for (int z = z1; z <= z2; z++) {
                kit.set(x1, y, z, BuildKit.s(planks));
                kit.set(x2, y, z, BuildKit.s(planks));
            }
            kit.fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, BuildKit.AIR);
        }
        for (int[] c : new int[][] {{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}}) {
            kit.fill(c[0], y0 + 1, c[1], c[0], y0 + 4, c[1], BuildKit.s(log));
        }
        // двери и окна
        kit.door(door, cx, y0 + 1, z2, Direction.SOUTH);
        for (int[] w : new int[][] {{cx - 2, z1}, {cx + 2, z1}, {x1, cz}, {x2, cz}, {cx - 2, z2}, {cx + 2, z2}}) {
            kit.set(w[0], y0 + 2, w[1], BuildKit.s(Blocks.GLASS_PANE));
        }
        // двускатная крыша по оси x
        for (int k = 0; k <= 2; k++) {
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                kit.set(x, y0 + 4 + k, z1 - 1 + k, BuildKit.stairs(stairs, Direction.SOUTH, false));
                kit.set(x, y0 + 4 + k, z2 + 1 - k, BuildKit.stairs(stairs, Direction.NORTH, false));
            }
        }
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            kit.set(x, y0 + 7, cz, BuildKit.s(slab));
        }
        for (int k = 0; k <= 2; k++) {
            for (int x : new int[] {x1, x2}) {
                for (int z = z1 + 1 + k; z <= z2 - 1 - k; z++) {
                    kit.set(x, y0 + 4 + k, z, BuildKit.s(planks));
                }
            }
        }
        // обстановка
        kit.set(x1 + 1, y0 + 1, z1 + 1, BuildKit.s(Blocks.CRAFTING_TABLE));
        kit.set(x1 + 2, y0 + 1, z1 + 1, BuildKit.s(Blocks.FURNACE));
        kit.set(x2 - 1, y0 + 1, z1 + 1, BuildKit.s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH));
        kit.set(x2 - 1, y0 + 1, z2 - 1, BuildKit.s(Blocks.BARREL));
        BlockState foot = Blocks.RED_BED.defaultBlockState()
                .setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.WEST)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT);
        BlockState head = foot.setValue(net.minecraft.world.level.block.BedBlock.PART,
                net.minecraft.world.level.block.state.properties.BedPart.HEAD);
        kit.set(x1 + 2, y0 + 1, z2 - 1, foot);
        kit.set(x1 + 1, y0 + 1, z2 - 1, head);
        kit.set(cx, y0 + 3, cz, BuildKit.HANGING_LANTERN);
        kit.set(cx, y0 + 4, cz, BuildKit.s(Blocks.CHAIN));
        // крыльцо
        kit.set(cx, y0, z2 + 1, BuildKit.s(Blocks.COBBLESTONE_SLAB));
        kit.set(cx - 2, y0 + 1, z2 + 1, BuildKit.s(Blocks.BARREL));
        kit.set(cx + 2, y0 + 1, z2 + 1, BuildKit.s(Blocks.OAK_FENCE));
        kit.set(cx + 2, y0 + 2, z2 + 1, BuildKit.s(Blocks.LANTERN));
    }

    private static void camp(BuildKit kit, Random rnd, int cx, int cz, int y0) {
        kit.set(cx, y0 + 1, cz, BuildKit.s(Blocks.CAMPFIRE));
        for (int[] p : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            kit.set(cx + p[0], y0 + 1, cz + p[1], BuildKit.s(Blocks.OAK_LOG));
        }
        // палатка из шерсти
        Block wool = rnd.nextBoolean() ? Blocks.RED_WOOL : Blocks.GRAY_WOOL;
        for (int i = 0; i < 5; i++) {
            int h = 1 + Math.min(i, 4 - i);
            for (int dx = 0; dx < 4; dx++) {
                for (int y = 1; y <= h; y++) {
                    if (y == h || dx == 0 || dx == 3) {
                        kit.set(cx + 3 + dx, y0 + y, cz - 2 + i, BuildKit.s(wool));
                    }
                }
            }
        }
        kit.set(cx - 3, y0 + 1, cz + 3, BuildKit.s(Blocks.BARREL));
        kit.set(cx - 3, y0 + 1, cz + 2, BuildKit.s(Blocks.HAY_BLOCK));
        kit.set(cx - 4, y0 + 1, cz + 3, BuildKit.s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.EAST));
        kit.fill(cx - 4, y0 + 1, cz - 3, cx - 4, y0 + 3, cz - 3, BuildKit.s(Blocks.OAK_FENCE));
        kit.set(cx - 4, y0 + 4, cz - 3, BuildKit.s(Blocks.LANTERN));
    }

    private static void shrine(BuildKit kit, int cx, int cz, int y0) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                kit.set(cx + dx, y0, cz + dz, ((dx + dz) & 1) == 0 ? BuildKit.s(Blocks.STONE_BRICKS) : BuildKit.s(Blocks.MOSSY_STONE_BRICKS));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                kit.set(cx + dx, y0 + 1, cz + dz, BuildKit.s(Blocks.CHISELED_STONE_BRICKS));
            }
        }
        kit.fill(cx, y0 + 2, cz, cx, y0 + 4, cz, BuildKit.s(Blocks.STONE_BRICK_WALL));
        kit.set(cx, y0 + 5, cz, BuildKit.s(Blocks.SEA_LANTERN));
        for (int[] p : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            kit.fill(cx + p[0], y0 + 1, cz + p[1], cx + p[0], y0 + 2, cz + p[1], BuildKit.s(Blocks.STONE_BRICK_WALL));
            kit.set(cx + p[0], y0 + 3, cz + p[1], BuildKit.s(Blocks.SOUL_LANTERN));
        }
        for (int[] p : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            kit.set(cx + p[0], y0 + 1, cz + p[1], BuildKit.s(Blocks.CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        }
    }

    private static void ruin(BuildKit kit, Random rnd, int cx, int cz, int y0) {
        for (int a = 0; a < 360; a += 12) {
            int x = cx + (int) Math.round(4.0D * Math.cos(Math.toRadians(a)));
            int z = cz + (int) Math.round(4.0D * Math.sin(Math.toRadians(a)));
            int h = 1 + rnd.nextInt(6);
            for (int y = 1; y <= h; y++) {
                kit.set(x, y0 + y, z, rnd.nextInt(3) == 0 ? BuildKit.s(Blocks.MOSSY_STONE_BRICKS) : BuildKit.s(Blocks.CRACKED_STONE_BRICKS));
            }
        }
        for (int i = 0; i < 10; i++) {
            kit.set(cx - 2 + rnd.nextInt(5), y0 + 1, cz - 2 + rnd.nextInt(5), BuildKit.s(Blocks.COBBLESTONE));
        }
        kit.set(cx, y0 + 1, cz, BuildKit.s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH));
        kit.set(cx + 1, y0 + 1, cz, BuildKit.s(Blocks.COBWEB));
    }

    private static void tower(BuildKit kit, int cx, int cz, int y0) {
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            kit.fill(cx + c[0], y0 + 1, cz + c[1], cx + c[0], y0 + 10, cz + c[1], BuildKit.s(Blocks.SPRUCE_LOG));
        }
        for (int y = y0 + 2; y <= y0 + 8; y += 3) {
            for (int d = -1; d <= 1; d++) {
                kit.set(cx + d, y, cz - 2, BuildKit.s(Blocks.SPRUCE_PLANKS));
                kit.set(cx + d, y, cz + 2, BuildKit.s(Blocks.SPRUCE_PLANKS));
                kit.set(cx - 2, y, cz + d, BuildKit.s(Blocks.SPRUCE_PLANKS));
                kit.set(cx + 2, y, cz + d, BuildKit.s(Blocks.SPRUCE_PLANKS));
            }
        }
        kit.fill(cx - 2, y0 + 8, cz - 2, cx + 2, y0 + 8, cz + 2, BuildKit.s(Blocks.SPRUCE_PLANKS));
        for (int y = y0 + 1; y <= y0 + 8; y++) {
            kit.set(cx, y, cz - 1, BuildKit.s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        kit.set(cx, y0 + 8, cz - 1, BuildKit.AIR);
        for (int d = -2; d <= 2; d++) {
            kit.set(cx + d, y0 + 9, cz - 2, BuildKit.s(Blocks.SPRUCE_FENCE));
            kit.set(cx + d, y0 + 9, cz + 2, BuildKit.s(Blocks.SPRUCE_FENCE));
            kit.set(cx - 2, y0 + 9, cz + d, BuildKit.s(Blocks.SPRUCE_FENCE));
            kit.set(cx + 2, y0 + 9, cz + d, BuildKit.s(Blocks.SPRUCE_FENCE));
        }
        kit.set(cx, y0 + 9, cz, BuildKit.s(Blocks.LANTERN));
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 4) {
                    kit.set(cx + dx, y0 + 12, cz + dz, BuildKit.s(Blocks.SPRUCE_SLAB));
                }
            }
        }
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            kit.set(cx + c[0], y0 + 11, cz + c[1], BuildKit.s(Blocks.SPRUCE_FENCE));
        }
        kit.set(cx, y0 + 1, cz + 2, BuildKit.s(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.NORTH));
    }
}
