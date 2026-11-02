package com.example.fateapocrypha.scene;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Локации сюжета, генерируемые кодом в измерении Трифас (плоский мир, пол на y = Y0).
 * Их можно заменить своими постройками (Structure Block), поменяв координаты в StoryScenes.
 *
 *  Лаборатория        x100-149, z100-114   |  Коридор падения   x100-130, z298-306
 *  Покои Хирона       x100-114, z400-414   |  Путь побега       x114-208, z398-416
 *  Лес (с рельефом)   x500-659, z350-509   |  Поляна боя        центр (520, 430)
 */
public class StoryWorld {
    public static final int Y0 = -60;

    public static final int FOREST_X0 = 500;
    public static final int FOREST_Z0 = 350;
    public static final int FOREST_SIZE = 160;
    public static final int FOREST_CX = 520;
    public static final int FOREST_CZ = 430;

    private record Theme(BlockState wall, BlockState wallAlt, BlockState floor, BlockState floorAlt, BlockState ceil) {
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static final BlockState AIR = s(Blocks.AIR);
    private static final BlockState LAMP = s(Blocks.SEA_LANTERN);
    private static final BlockState GLASS = s(Blocks.LIGHT_BLUE_STAINED_GLASS);
    private static final BlockState BARS = s(Blocks.IRON_BARS);
    private static final BlockState CHAIN = s(Blocks.CHAIN);
    private static final BlockState COPPER = s(Blocks.CUT_COPPER);
    private static final BlockState CARPET_RED = s(Blocks.RED_CARPET);
    private static final BlockState HANGING_LANTERN =
            Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

    private static final Theme LAB = new Theme(s(Blocks.POLISHED_BLACKSTONE_BRICKS),
            s(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS), s(Blocks.POLISHED_BLACKSTONE),
            s(Blocks.POLISHED_ANDESITE), s(Blocks.POLISHED_BLACKSTONE_BRICKS));
    private static final Theme CASTLE = new Theme(s(Blocks.STONE_BRICKS), s(Blocks.CRACKED_STONE_BRICKS),
            s(Blocks.POLISHED_ANDESITE), s(Blocks.ANDESITE), s(Blocks.STONE_BRICKS));
    private static final Theme CHAMBER = new Theme(s(Blocks.STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS),
            s(Blocks.SPRUCE_PLANKS), s(Blocks.DARK_OAK_PLANKS), s(Blocks.SPRUCE_PLANKS));

    // ================= Построение =================
    public static void ensureBuilt(ServerLevel level) {
        if (!level.getBlockState(new BlockPos(100, Y0 + 1, 100)).is(Blocks.CHISELED_POLISHED_BLACKSTONE)) {
            buildCastle(level);
        }
    }

    public static void ensureForest(ServerLevel level) {
        if (level != null && !level.getBlockState(new BlockPos(FOREST_CX, Y0 - 1, FOREST_CZ)).is(Blocks.COARSE_DIRT)) {
            buildForest(level);
        }
    }

    public static void buildAll(ServerLevel l) {
        buildCastle(l);
        buildForest(l);
        buildStages(l);
    }

    private static void buildCastle(ServerLevel l) {
        buildLab(l);
        buildFallCorridor(l);
        buildChironRoom(l);
        buildEscapeRoute(l);
        set(l, 100, Y0 + 1, 100, s(Blocks.CHISELED_POLISHED_BLACKSTONE));   // метка "построено"
    }

    // ================= Помощники =================
    private static void set(ServerLevel l, int x, int y, int z, BlockState state) {
        l.setBlock(new BlockPos(x, y, z), state, 2);
    }

    private static void setSoft(ServerLevel l, int x, int y, int z, BlockState state) {
        l.setBlock(new BlockPos(x, y, z), state, 3);   // с обновлением соседей (решётки, панели)
    }

    private static void fill(ServerLevel l, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    l.setBlock(pos.set(x, y, z), state, 2);
                }
            }
        }
    }

    private static void fillMixed(ServerLevel l, int x1, int y1, int z1, int x2, int y2, int z2,
                                  BlockState a, BlockState b, double chanceB, Random rnd) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    l.setBlock(pos.set(x, y, z), rnd.nextDouble() < chanceB ? b : a, 2);
                }
            }
        }
    }

    /** Закрытая комната: потрескавшиеся стены, шахматный пол, потолок; внутри h блоков высоты. */
    private static void room(ServerLevel l, int x1, int z1, int x2, int z2, int h, Theme t) {
        Random rnd = new Random(x1 * 31L + z1 * 17L);
        fillMixed(l, x1, Y0, z1, x2, Y0 + h + 1, z2, t.wall(), t.wallAlt(), 0.08D, rnd);
        fill(l, x1 + 1, Y0 + 1, z1 + 1, x2 - 1, Y0 + h, z2 - 1, AIR);
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                set(l, x, Y0, z, ((x + z) & 1) == 0 ? t.floor() : t.floorAlt());
            }
        }
        fill(l, x1, Y0 + h + 1, z1, x2, Y0 + h + 1, z2, t.ceil());
    }

    private static void doorX(ServerLevel l, int x, int z1, int z2, int h) {
        fill(l, x, Y0 + 1, z1, x, Y0 + h, z2, AIR);
    }

    private static void lampRow(ServerLevel l, int x1, int x2, int z, int h, int step) {
        for (int x = x1; x <= x2; x += step) {
            set(l, x, Y0 + h + 1, z, LAMP);
        }
    }

    private static void pillar(ServerLevel l, int x, int z, int h, BlockState state) {
        fill(l, x, Y0 + 1, z, x, Y0 + h, z, state);
    }

    // ================= Лаборатория =================
    private static void buildLab(ServerLevel l) {
        room(l, 100, 100, 114, 114, 6, LAB);

        // помост под главным резервуаром
        fill(l, 104, Y0, 104, 110, Y0, 110, s(Blocks.SMOOTH_QUARTZ));
        fill(l, 105, Y0, 105, 109, Y0, 109, s(Blocks.IRON_BLOCK));

        // потолочные лампы и цепи, на которых висит резервуар
        for (int x : new int[] {103, 107, 111}) {
            for (int z : new int[] {103, 107, 111}) {
                if (x != 107 || z != 107) {
                    set(l, x, Y0 + 7, z, LAMP);
                }
            }
        }
        for (int[] c : new int[][] {{105, 105}, {105, 109}, {109, 105}, {109, 109}, {107, 107}}) {
            set(l, c[0], Y0 + 6, c[1], CHAIN);
        }
        repairTank(l);

        // малые резервуары по углам: в двух из них жидкость
        for (int[] c : new int[][] {{103, 103}, {103, 111}, {111, 103}, {111, 111}}) {
            fill(l, c[0] - 1, Y0 + 1, c[1] - 1, c[0] + 1, Y0 + 5, c[1] + 1, GLASS);
            boolean filled = (c[0] == 103 && c[1] == 103) || (c[0] == 111 && c[1] == 111);
            fill(l, c[0], Y0 + 1, c[1], c[0], Y0 + 4, c[1], filled ? s(Blocks.WATER) : AIR);
            fill(l, c[0] - 1, Y0, c[1] - 1, c[0] + 1, Y0, c[1] + 1, s(Blocks.IRON_BLOCK));
        }

        // пульты вдоль северной и южной стен (светящиеся экраны)
        for (int x = 102; x <= 112; x++) {
            for (int z : new int[] {101, 113}) {
                set(l, x, Y0 + 1, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
                set(l, x, Y0 + 2, z, x % 2 == 0 ? LAMP : s(Blocks.CYAN_STAINED_GLASS));
            }
        }
        // медные трубы в углах
        for (int[] c : new int[][] {{101, 101}, {101, 113}, {113, 101}, {113, 113}}) {
            pillar(l, c[0], c[1], 6, COPPER);
        }

        // коридор
        room(l, 114, 105, 135, 109, 4, LAB);
        fill(l, 115, Y0, 107, 134, Y0, 107, s(Blocks.GRAY_CONCRETE));
        lampRow(l, 118, 132, 107, 4, 4);
        for (int x = 117; x <= 133; x += 4) {
            set(l, x, Y0 + 3, 105, LAMP);
            set(l, x, Y0 + 3, 109, LAMP);
        }
        for (int x = 119; x <= 131; x += 6) {
            setSoft(l, x, Y0 + 2, 105, s(Blocks.CYAN_STAINED_GLASS_PANE));
            setSoft(l, x, Y0 + 2, 109, s(Blocks.CYAN_STAINED_GLASS_PANE));
        }

        // зал выхода
        room(l, 135, 100, 149, 114, 6, LAB);
        for (int x : new int[] {139, 145}) {
            for (int z : new int[] {102, 112}) {
                pillar(l, x, z, 6, COPPER);
            }
        }
        for (int x = 137; x <= 147; x += 2) {
            set(l, x, Y0 + 1, 101, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            set(l, x, Y0 + 2, 101, LAMP);
            set(l, x, Y0 + 1, 113, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            set(l, x, Y0 + 2, 113, LAMP);
        }
        set(l, 142, Y0 + 7, 107, LAMP);
        set(l, 142, Y0 + 7, 103, LAMP);
        set(l, 142, Y0 + 7, 111, LAMP);
        doorX(l, 114, 106, 108, 4);
        doorX(l, 135, 106, 108, 4);
        fill(l, 149, Y0 + 1, 105, 149, Y0 + 5, 109, s(Blocks.IRON_BLOCK));   // рама выхода
        fill(l, 149, Y0 + 1, 106, 149, Y0 + 4, 108, LAMP);                  // свет в конце
    }

    /** Стекло резервуара: 5x5 снаружи, пустой 3x3 внутри. */
    public static void repairTank(ServerLevel l) {
        fill(l, 105, Y0 + 1, 105, 109, Y0 + 5, 109, GLASS);
        fill(l, 106, Y0 + 1, 106, 108, Y0 + 4, 108, AIR);
    }

    public static boolean isTankBroken(ServerLevel l) {
        if (l == null) {
            return false;
        }
        for (int y = Y0 + 1; y <= Y0 + 4; y++) {
            for (int i = 105; i <= 109; i++) {
                if (l.getBlockState(new BlockPos(i, y, 105)).isAir()
                        || l.getBlockState(new BlockPos(i, y, 109)).isAir()
                        || l.getBlockState(new BlockPos(105, y, i)).isAir()
                        || l.getBlockState(new BlockPos(109, y, i)).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ================= Коридор падения =================
    private static void buildFallCorridor(ServerLevel l) {
        room(l, 100, 298, 130, 306, 6, LAB);
        fill(l, 101, Y0 + 1, 302, 129, Y0 + 1, 302, s(Blocks.BLACK_CARPET));
        for (int x = 105; x <= 125; x += 5) {
            pillar(l, x, 299, 6, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            pillar(l, x, 305, 6, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            set(l, x, Y0 + 7, 302, LAMP);
            set(l, x, Y0 + 4, 298, LAMP);
            set(l, x, Y0 + 4, 306, LAMP);
        }
    }

    // ================= Покои Хирона =================
    private static void buildChironRoom(ServerLevel l) {
        room(l, 100, 400, 114, 414, 5, CHAMBER);

        // ковёр и светильники
        fill(l, 104, Y0 + 1, 405, 109, Y0 + 1, 409, CARPET_RED);
        for (int[] p : new int[][] {{104, 404}, {104, 410}, {110, 404}, {110, 410}, {107, 407}}) {
            set(l, p[0], Y0 + 5, p[1], HANGING_LANTERN);
        }

        // библиотека: северная и западная стены
        fill(l, 102, Y0 + 1, 401, 112, Y0 + 3, 401, s(Blocks.BOOKSHELF));
        fill(l, 101, Y0 + 1, 402, 101, Y0 + 3, 412, s(Blocks.BOOKSHELF));

        // южная стена: мастерская целителя
        set(l, 103, Y0 + 1, 413, s(Blocks.CRAFTING_TABLE));
        set(l, 104, Y0 + 1, 413, s(Blocks.FURNACE));
        set(l, 105, Y0 + 1, 413, s(Blocks.BREWING_STAND));
        set(l, 106, Y0 + 1, 413, s(Blocks.CAULDRON));
        set(l, 108, Y0 + 1, 413, s(Blocks.LECTERN));
        set(l, 110, Y0 + 1, 413, s(Blocks.CARTOGRAPHY_TABLE));
        for (int x = 103; x <= 107; x++) {
            setSoft(l, x, Y0 + 2, 414, s(Blocks.GLASS_PANE));
            setSoft(l, x, Y0 + 3, 414, s(Blocks.GLASS_PANE));
        }
        set(l, 113, Y0 + 1, 401, s(Blocks.POTTED_FERN));
        set(l, 113, Y0 + 1, 413, s(Blocks.POTTED_FERN));
        set(l, 102, Y0 + 1, 413, s(Blocks.POTTED_FERN));

        // кровать
        BlockState foot = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT);
        BlockState head = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD);
        set(l, 110, Y0 + 1, 407, foot);
        set(l, 111, Y0 + 1, 407, head);
    }

    // ================= Путь побега =================
    private static void buildEscapeRoute(ServerLevel l) {
        // длинный коридор с ковром, окнами-решётками и подвесными фонарями
        room(l, 114, 405, 160, 409, 4, CASTLE);
        fill(l, 115, Y0 + 1, 407, 159, Y0 + 1, 407, CARPET_RED);
        for (int x = 118; x <= 156; x += 5) {
            set(l, x, Y0 + 4, 407, HANGING_LANTERN);
        }
        for (int x = 120; x <= 154; x += 6) {
            setSoft(l, x, Y0 + 3, 405, BARS);
            setSoft(l, x, Y0 + 3, 409, BARS);
        }

        // большой зал: колонны, ковёр, люстры, окна
        room(l, 160, 398, 178, 416, 7, CASTLE);
        fill(l, 161, Y0 + 1, 407, 177, Y0 + 1, 407, CARPET_RED);
        fill(l, 161, Y0 + 1, 406, 177, Y0 + 1, 406, CARPET_RED);
        fill(l, 161, Y0 + 1, 408, 177, Y0 + 1, 408, CARPET_RED);
        for (int x : new int[] {164, 169, 174}) {
            for (int z : new int[] {402, 412}) {
                pillar(l, x, z, 7, s(Blocks.STONE_BRICKS));
                set(l, x, Y0 + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
                set(l, x, Y0 + 7, z, s(Blocks.CHISELED_STONE_BRICKS));
            }
        }
        for (int x : new int[] {166, 172}) {
            set(l, x, Y0 + 7, 407, CHAIN);
            set(l, x, Y0 + 6, 407, CHAIN);
            set(l, x, Y0 + 5, 407, HANGING_LANTERN);
        }
        for (int x = 163; x <= 175; x += 3) {
            for (int y = Y0 + 4; y <= Y0 + 6; y++) {
                setSoft(l, x, y, 398, BARS);
                setSoft(l, x, y, 416, BARS);
            }
        }

        // коридор к воротам
        room(l, 178, 405, 191, 409, 4, CASTLE);
        fill(l, 179, Y0 + 1, 407, 190, Y0 + 1, 407, CARPET_RED);
        for (int x = 181; x <= 189; x += 4) {
            set(l, x, Y0 + 4, 407, HANGING_LANTERN);
        }

        doorX(l, 114, 406, 408, 4);
        doorX(l, 160, 406, 408, 4);
        doorX(l, 178, 406, 408, 4);

        // рама ворот и двор снаружи
        fill(l, 191, Y0 + 1, 405, 191, Y0 + 4, 409, s(Blocks.CHISELED_STONE_BRICKS));
        closeGate(l);
        fill(l, 192, Y0, 404, 208, Y0, 410, s(Blocks.STONE_BRICKS));
        for (int x : new int[] {195, 203}) {
            for (int z : new int[] {404, 410}) {
                set(l, x, Y0 + 1, z, s(Blocks.COBBLESTONE_WALL));
                set(l, x, Y0 + 2, z, s(Blocks.COBBLESTONE_WALL));
                set(l, x, Y0 + 3, z, s(Blocks.LANTERN));
            }
        }
    }

    public static void closeGate(ServerLevel l) {
        for (int z = 406; z <= 408; z++) {
            for (int y = Y0 + 1; y <= Y0 + 3; y++) {
                setSoft(l, 191, y, z, BARS);
            }
        }
    }

    public static void openGate(ServerLevel l) {
        fill(l, 191, Y0 + 1, 406, 191, Y0 + 3, 408, AIR);
    }

    // ================= Лес с рельефом =================
    private static double hash01(int x, int z) {
        long h = x * 374761393L + z * 668265263L + 1337L * 1442695040888963407L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= (h >>> 16);
        return (h & 0xFFFFL) / 65535.0D;
    }

    private static double noise(double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        fx = fx * fx * (3.0D - 2.0D * fx);
        fz = fz * fz * (3.0D - 2.0D * fz);
        double a = hash01(x0, z0);
        double b = hash01(x0 + 1, z0);
        double c = hash01(x0, z0 + 1);
        double d = hash01(x0 + 1, z0 + 1);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    private static double baseNoise(int x, int z) {
        return noise(x / 32.0D, z / 32.0D) * 0.6D + noise(x / 14.0D, z / 14.0D) * 0.3D
                + noise(x / 6.0D, z / 6.0D) * 0.1D;
    }

    private static double clearingDistance(int x, int z) {
        return Math.hypot(x - FOREST_CX, z - FOREST_CZ);
    }

    private static boolean isPond(int x, int z) {
        return clearingDistance(x, z) > 14.0D && baseNoise(x, z) < 0.20D;
    }

    /** Высота холмов над полом (0 = ровное место). Поляна боя и края леса плоские. */
    private static int heightAt(int x, int z) {
        double h = Math.max(0.0D, (baseNoise(x, z) - 0.30D) * 24.0D);
        h *= clamp((clearingDistance(x, z) - 10.0D) / 14.0D);
        int edge = Math.min(Math.min(x - FOREST_X0, FOREST_X0 + FOREST_SIZE - 1 - x),
                Math.min(z - FOREST_Z0, FOREST_Z0 + FOREST_SIZE - 1 - z));
        h *= clamp(edge / 12.0D);
        return (int) Math.round(h);
    }

    private static double clamp(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }

    private static void buildForest(ServerLevel l) {
        int x0 = FOREST_X0;
        int z0 = FOREST_Z0;
        int size = FOREST_SIZE;
        Random rnd = new Random(2024L);

        // 1. рельеф: холмы, скалы на вершинах, пруды в низинах
        for (int x = x0; x < x0 + size; x++) {
            for (int z = z0; z < z0 + size; z++) {
                if (isPond(x, z)) {
                    set(l, x, Y0 - 1, z, s(Blocks.SAND));
                    set(l, x, Y0, z, s(Blocks.WATER));
                    continue;
                }
                int h = heightAt(x, z);
                for (int y = 1; y <= h; y++) {
                    BlockState state;
                    if (y == h) {
                        state = h >= 12 ? (rnd.nextInt(4) == 0 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.STONE))
                                : s(Blocks.GRASS_BLOCK);
                    } else if (y >= h - 3) {
                        state = h >= 12 ? s(Blocks.STONE) : s(Blocks.DIRT);
                    } else {
                        state = s(Blocks.STONE);
                    }
                    set(l, x, Y0 + y, z, state);
                }
            }
        }

        // 2. деревья, валуны
        for (int i = 0; i < 520; i++) {
            int x = x0 + 6 + rnd.nextInt(size - 12);
            int z = z0 + 6 + rnd.nextInt(size - 12);
            if (clearingDistance(x, z) < 13.0D || isPond(x, z)) {
                continue;
            }
            int h = heightAt(x, z);
            if (h >= 11) {
                continue;
            }
            int baseY = Y0 + h + 1;
            int kind = rnd.nextInt(10);
            if (kind <= 5) {
                oak(l, x, baseY, z, 4 + rnd.nextInt(3));
            } else if (kind <= 8) {
                spruce(l, x, baseY, z, 6 + rnd.nextInt(4));
            } else {
                boulder(l, x, baseY, z, 1 + rnd.nextInt(2), rnd);
            }
        }

        // 3. трава и цветы
        BlockState[] cover = {s(Blocks.GRASS), s(Blocks.GRASS), s(Blocks.GRASS), s(Blocks.FERN),
                s(Blocks.DANDELION), s(Blocks.POPPY), s(Blocks.AZURE_BLUET), s(Blocks.CORNFLOWER)};
        for (int i = 0; i < 3500; i++) {
            int x = x0 + 2 + rnd.nextInt(size - 4);
            int z = z0 + 2 + rnd.nextInt(size - 4);
            if (isPond(x, z) || heightAt(x, z) >= 12) {
                continue;
            }
            BlockPos pos = new BlockPos(x, Y0 + heightAt(x, z) + 1, z);
            if (l.getBlockState(pos).isAir() && l.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                l.setBlock(pos, cover[rnd.nextInt(cover.length)], 2);
            }
        }

        set(l, FOREST_CX, Y0 - 1, FOREST_CZ, s(Blocks.COARSE_DIRT));   // метка "лес построен"
    }

    private static BlockState leaves(Block block) {
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void oak(ServerLevel l, int x, int baseY, int z, int height) {
        int top = baseY + height - 1;
        BlockState leaf = leaves(Blocks.OAK_LEAVES);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        continue;
                    }
                    if (dy == 2 && (Math.abs(dx) > 1 || Math.abs(dz) > 1)) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x + dx, top + dy, z + dz);
                    if (l.getBlockState(pos).isAir()) {
                        l.setBlock(pos, leaf, 2);
                    }
                }
            }
        }
        for (int y = baseY; y <= top; y++) {
            set(l, x, y, z, s(Blocks.OAK_LOG));
        }
    }

    private static void spruce(ServerLevel l, int x, int baseY, int z, int height) {
        int top = baseY + height - 1;
        BlockState leaf = leaves(Blocks.SPRUCE_LEAVES);
        for (int i = 0; i <= height - 2; i++) {
            int y = top + 1 - i;
            int r = Math.min(3, (i + 1) / 2);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > r + (r > 1 ? 1 : 0)) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x + dx, y, z + dz);
                    if (l.getBlockState(pos).isAir()) {
                        l.setBlock(pos, leaf, 2);
                    }
                }
            }
        }
        for (int y = baseY; y <= top; y++) {
            set(l, x, y, z, s(Blocks.SPRUCE_LOG));
        }
    }

    private static void boulder(ServerLevel l, int x, int baseY, int z, int r, Random rnd) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= r * r + 1) {
                        set(l, x + dx, baseY + dy, z + dz,
                                rnd.nextBoolean() ? s(Blocks.COBBLESTONE) : s(Blocks.MOSSY_COBBLESTONE));
                    }
                }
            }
        }
    }

    // ================= Сцены интерлюдий (смотрим со стороны, камерой) =================
    public static final int STAGE_RED_X = 800;       // лагерь Красной фракции
    public static final int STAGE_RED_Z = 800;
    public static final int STAGE_CHAPEL_X = 800;    // церковь Широ: x790-810, z890-920
    public static final int STAGE_CHAPEL_Z = 905;
    public static final int STAGE_RUINS_X = 800;     // руины для боя Франкенштейн и Мордред
    public static final int STAGE_RUINS_Z = 1000;

    public static void ensureStages(ServerLevel l) {
        if (l != null && !l.getBlockState(new BlockPos(STAGE_RED_X, Y0 - 1, STAGE_RED_Z)).is(Blocks.COARSE_DIRT)) {
            buildStages(l);
        }
    }

    private static void buildStages(ServerLevel l) {
        buildRedCamp(l);
        buildChapel(l);
        buildRuins(l);
        set(l, STAGE_RED_X, Y0 - 1, STAGE_RED_Z, s(Blocks.COARSE_DIRT));   // метка "построено"
    }

    private static void buildRedCamp(ServerLevel l) {
        int cx = STAGE_RED_X;
        int cz = STAGE_RED_Z;
        Random rnd = new Random(7L);
        for (int x = cx - 18; x <= cx + 18; x++) {
            for (int z = cz - 18; z <= cz + 18; z++) {
                if (Math.hypot(x - cx, z - cz) <= 16.0D) {
                    set(l, x, Y0, z, rnd.nextInt(3) == 0 ? s(Blocks.PODZOL) : s(Blocks.COARSE_DIRT));
                }
            }
        }
        set(l, cx, Y0 + 1, cz, s(Blocks.CAMPFIRE));
        set(l, cx - 3, Y0 + 1, cz, s(Blocks.HAY_BLOCK));
        set(l, cx + 3, Y0 + 1, cz, s(Blocks.HAY_BLOCK));
        set(l, cx, Y0 + 1, cz - 3, s(Blocks.HAY_BLOCK));
        tent(l, cx - 16, cz - 10, s(Blocks.RED_WOOL));
        tent(l, cx - 16, cz + 6, s(Blocks.GRAY_WOOL));
        tent(l, cx + 11, cz - 12, s(Blocks.RED_WOOL));
        set(l, cx + 7, Y0 + 1, cz - 7, s(Blocks.BARREL));
        set(l, cx + 8, Y0 + 1, cz - 7, s(Blocks.BARREL));
        set(l, cx + 7, Y0 + 2, cz - 7, s(Blocks.BARREL));
        for (int[] c : new int[][] {{-11, -11}, {11, -11}, {-11, 11}, {11, 11}}) {
            set(l, cx + c[0], Y0 + 1, cz + c[1], s(Blocks.OAK_FENCE));
            set(l, cx + c[0], Y0 + 2, cz + c[1], s(Blocks.OAK_FENCE));
            set(l, cx + c[0], Y0 + 3, cz + c[1], s(Blocks.LANTERN));
        }
    }

    /** Палатка-шатёр: двускатная крыша из шерсти (ширина 6 по x, длина 5 по z). */
    private static void tent(ServerLevel l, int x, int z, BlockState wool) {
        for (int i = 0; i < 5; i++) {
            int h = 1 + Math.min(i, 4 - i);
            for (int dx = 0; dx < 6; dx++) {
                for (int y = 1; y <= h; y++) {
                    if (y == h || dx == 0 || dx == 5) {
                        set(l, x + dx, Y0 + y, z + i, wool);
                    }
                }
            }
        }
    }

    private static void buildChapel(ServerLevel l) {
        room(l, 790, 890, 810, 920, 10, CASTLE);
        fill(l, 799, Y0 + 1, 892, 801, Y0 + 1, 918, CARPET_RED);
        for (int z = 896; z <= 916; z += 5) {
            pillar(l, 794, z, 10, s(Blocks.STONE_BRICKS));
            pillar(l, 806, z, 10, s(Blocks.STONE_BRICKS));
        }
        for (int z : new int[] {897, 903, 909, 915}) {
            fill(l, 790, Y0 + 3, z, 790, Y0 + 8, z + 1, s(Blocks.PURPLE_STAINED_GLASS));
            fill(l, 810, Y0 + 3, z, 810, Y0 + 8, z + 1, s(Blocks.PURPLE_STAINED_GLASS));
        }
        for (int z = 898; z <= 912; z += 2) {
            for (int x : new int[] {796, 797, 798, 802, 803, 804}) {
                set(l, x, Y0 + 1, z, s(Blocks.SPRUCE_SLAB));
            }
        }
        fill(l, 798, Y0 + 1, 915, 802, Y0 + 1, 915, s(Blocks.POLISHED_ANDESITE));
        fill(l, 799, Y0 + 2, 915, 801, Y0 + 2, 915, s(Blocks.SMOOTH_QUARTZ_SLAB));
        BlockState candle = Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
        set(l, 798, Y0 + 2, 915, candle);
        set(l, 802, Y0 + 2, 915, candle);
        for (int z : new int[] {898, 905, 912}) {
            set(l, 800, Y0 + 10, z, CHAIN);
            set(l, 800, Y0 + 9, z, CHAIN);
            set(l, 800, Y0 + 8, z, HANGING_LANTERN);
        }
    }

    private static void buildRuins(ServerLevel l) {
        int cx = STAGE_RUINS_X;
        int cz = STAGE_RUINS_Z;
        Random rnd = new Random(99L);
        for (int x = cx - 20; x <= cx + 20; x++) {
            for (int z = cz - 20; z <= cz + 20; z++) {
                int r = rnd.nextInt(10);
                if (r < 2) {
                    set(l, x, Y0, z, s(Blocks.COBBLESTONE));
                } else if (r == 2) {
                    set(l, x, Y0, z, s(Blocks.GRAVEL));
                } else if (r == 3) {
                    set(l, x, Y0, z, s(Blocks.MOSSY_COBBLESTONE));
                }
            }
        }
        // обломки стен только по краям, чтобы не мешать камере
        for (int i = 0; i < 22; i++) {
            int x = cx - 20 + rnd.nextInt(41);
            int z = cz - 20 + rnd.nextInt(41);
            if (Math.abs(x - cx) < 14 && Math.abs(z - cz) < 14) {
                continue;
            }
            int len = 3 + rnd.nextInt(5);
            boolean alongX = rnd.nextBoolean();
            int height = 2 + rnd.nextInt(4);
            for (int k = 0; k < len; k++) {
                int hh = height - rnd.nextInt(2);
                for (int y = 1; y <= hh; y++) {
                    int roll = rnd.nextInt(3);
                    BlockState state = roll == 0 ? s(Blocks.CRACKED_STONE_BRICKS)
                            : roll == 1 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
                    set(l, alongX ? x + k : x, Y0 + y, alongX ? z : z + k, state);
                }
            }
        }
        for (int[] c : new int[][] {{-14, -9}, {14, -9}, {-14, 9}, {14, 9}}) {
            set(l, cx + c[0], Y0 + 1, cz + c[1], s(Blocks.COBBLESTONE_WALL));
            set(l, cx + c[0], Y0 + 2, cz + c[1], s(Blocks.COBBLESTONE_WALL));
            set(l, cx + c[0], Y0 + 3, cz + c[1], s(Blocks.LANTERN));
        }
    }
}
