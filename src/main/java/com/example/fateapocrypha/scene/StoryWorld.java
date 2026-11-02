package com.example.fateapocrypha.scene;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Заглушечные локации сюжета, генерируемые кодом в измерении Трифас (плоский мир, пол на y = Y0).
 * Позже каждую можно заменить собственной постройкой (Structure Block), поменяв координаты в StoryScenes.
 *
 * Локации:  лаборатория (100,100)-(149,114) | коридор падения (100,300)-(130,304)
 *           покои Хирона (100,400)-(114,414) | путь побега (114,405)-(191,409) | лес (500,400)-(560,460)
 */
public class StoryWorld {
    public static final int Y0 = -60;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState FLOOR = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
    private static final BlockState BARS = Blocks.IRON_BARS.defaultBlockState();

    public static void ensureBuilt(ServerLevel level) {
        if (!level.getBlockState(new BlockPos(100, Y0 + 1, 100)).is(Blocks.POLISHED_BLACKSTONE_BRICKS)) {
            buildAll(level);
        }
    }

    public static void buildAll(ServerLevel l) {
        buildLab(l);
        buildFallCorridor(l);
        buildChironRoom(l);
        buildEscapeRoute(l);
        buildForest(l);
    }

    // ---------- Помощники ----------
    private static void set(ServerLevel l, int x, int y, int z, BlockState state) {
        l.setBlock(new BlockPos(x, y, z), state, 2);
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

    /** Закрытая комната: стены, пол, потолок; внутри h блоков высоты. */
    private static void room(ServerLevel l, int x1, int z1, int x2, int z2, int h) {
        fill(l, x1, Y0, z1, x2, Y0 + h + 1, z2, WALL);
        fill(l, x1 + 1, Y0 + 1, z1 + 1, x2 - 1, Y0 + h, z2 - 1, AIR);
        fill(l, x1, Y0, z1, x2, Y0, z2, FLOOR);
    }

    private static void lampRow(ServerLevel l, int x1, int x2, int z, int h, int step) {
        for (int x = x1; x <= x2; x += step) {
            set(l, x, Y0 + h + 1, z, LAMP);
        }
    }

    private static void doorX(ServerLevel l, int x, int z1, int z2, int h) {
        fill(l, x, Y0 + 1, z1, x, Y0 + h, z2, AIR);
    }

    // ---------- Лаборатория ----------
    private static void buildLab(ServerLevel l) {
        room(l, 100, 100, 114, 114, 6);                 // зал с резервуаром
        for (int x : new int[] {103, 107, 111}) {
            for (int z : new int[] {103, 107, 111}) {
                set(l, x, Y0 + 7, z, LAMP);
            }
        }
        repairTank(l);                                  // главный резервуар Зига
        for (int[] c : new int[][] {{103, 103}, {103, 111}, {111, 103}, {111, 111}}) {
            fill(l, c[0] - 1, Y0 + 1, c[1] - 1, c[0] + 1, Y0 + 5, c[1] + 1, GLASS);
            fill(l, c[0], Y0 + 1, c[1], c[0], Y0 + 4, c[1], AIR);
        }

        room(l, 114, 105, 135, 109, 4);                 // коридор
        lampRow(l, 118, 132, 107, 4, 4);
        room(l, 135, 100, 149, 114, 6);                 // зал выхода
        set(l, 142, Y0 + 7, 107, LAMP);
        doorX(l, 114, 106, 108, 4);
        doorX(l, 135, 106, 108, 4);
        fill(l, 149, Y0 + 1, 106, 149, Y0 + 4, 108, LAMP);   // светящийся "выход"
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

    // ---------- Коридор падения ----------
    private static void buildFallCorridor(ServerLevel l) {
        room(l, 100, 300, 130, 304, 4);
        lampRow(l, 104, 126, 302, 4, 5);
    }

    // ---------- Покои Хирона ----------
    private static void buildChironRoom(ServerLevel l) {
        room(l, 100, 400, 114, 414, 5);
        set(l, 104, Y0 + 6, 404, LAMP);
        set(l, 104, Y0 + 6, 410, LAMP);
        set(l, 110, Y0 + 6, 404, LAMP);
        set(l, 110, Y0 + 6, 410, LAMP);
        fill(l, 102, Y0 + 1, 401, 106, Y0 + 3, 401, Blocks.BOOKSHELF.defaultBlockState());
        set(l, 103, Y0 + 1, 412, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(l, 104, Y0 + 1, 412, Blocks.FURNACE.defaultBlockState());
        BlockState foot = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT);
        BlockState head = Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD);
        set(l, 110, Y0 + 1, 407, foot);
        set(l, 111, Y0 + 1, 407, head);
    }

    // ---------- Путь побега ----------
    private static void buildEscapeRoute(ServerLevel l) {
        room(l, 114, 405, 160, 409, 4);                 // длинный коридор
        lampRow(l, 118, 156, 407, 4, 5);
        room(l, 160, 400, 176, 414, 5);                 // зал засады
        set(l, 164, Y0 + 6, 404, LAMP);
        set(l, 164, Y0 + 6, 410, LAMP);
        set(l, 170, Y0 + 6, 404, LAMP);
        set(l, 170, Y0 + 6, 410, LAMP);
        room(l, 176, 405, 191, 409, 4);                 // коридор к воротам
        lampRow(l, 180, 188, 407, 4, 4);
        doorX(l, 114, 406, 408, 4);
        doorX(l, 160, 406, 408, 4);
        doorX(l, 176, 406, 408, 4);
        closeGate(l);
    }

    public static void closeGate(ServerLevel l) {
        for (int z = 406; z <= 408; z++) {
            for (int y = Y0 + 1; y <= Y0 + 3; y++) {
                l.setBlock(new BlockPos(191, y, z), BARS, 3);
            }
        }
    }

    public static void openGate(ServerLevel l) {
        fill(l, 191, Y0 + 1, 406, 191, Y0 + 3, 408, AIR);
    }

    // ---------- Лес ----------
    private static void buildForest(ServerLevel l) {
        Random rnd = new Random(1337L);
        for (int i = 0; i < 45; i++) {
            int x = 500 + rnd.nextInt(60);
            int z = 400 + rnd.nextInt(60);
            if (Math.abs(x - 505) < 6 && Math.abs(z - 430) < 6) {
                continue;
            }
            tree(l, x, z, 4 + rnd.nextInt(2));
        }
    }

    private static void tree(ServerLevel l, int x, int z, int height) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int top = Y0 + height;
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
                        l.setBlock(pos, leaves, 2);
                    }
                }
            }
        }
        for (int y = Y0 + 1; y <= top; y++) {
            set(l, x, y, z, log);
        }
    }
}
