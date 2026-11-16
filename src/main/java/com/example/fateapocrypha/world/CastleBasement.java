package com.example.fateapocrypha.world;

import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Подвал замка под большим залом и крыльями: тюрьма (8 камер), винный погреб, кухня, склад и алхимический погреб,
 * коридор между ними, тайная комната с сокровищами за потрескавшейся стеной погреба.
 * Входы: лестница из кухни в пиршественный зал и спуск из двора у южной стены корпуса.
 * Пол подвала на 6 блоков ниже первого этажа (Y-6), потолком служит пол первого этажа.
 */
final class CastleBasement {
    private static final int Y = CastleSite.Y;
    private static final int FY = Y - 6;   // блок пола подвала; стоять на нём можно на уровне FY+1
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ServerLevel level;
    private final BuildKit k;
    private final Random rnd = new Random(7771L);

    private CastleBasement(ServerLevel level) {
        this.level = level;
        this.k = new BuildKit(level);
    }

    static void build(ServerLevel level) {
        CastleBasement b = new CastleBasement(level);
        b.secretRoomShell();   // сначала тайная комната: общую стену потом перекроет погреб
        b.rooms();
        b.prison();
        b.cellar();
        b.secretRoom();
        b.kitchen();
        b.storage();
        b.alchemy();
        b.entrance();
        b.stairsToHall();
        b.lights();
    }

    // ================= помощники =================
    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState facing(Block b, Direction d) {
        return b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
    }

    private BlockState brick(int x, int y, int z) {
        int h = Math.floorMod(x * 374761393 + y * 668265263 + z * 1274126177, 100);
        if (h < 12) {
            return s(Blocks.CRACKED_STONE_BRICKS);
        }
        if (h < 26) {
            return s(Blocks.MOSSY_STONE_BRICKS);
        }
        return s(Blocks.STONE_BRICKS);
    }

    /** Комната: стены толщиной 1 вокруг (x1-1..x2+1), внутри пустота и пол из двух чередующихся блоков. */
    private void room(int x1, int z1, int x2, int z2, Block floorA, Block floorB, Block wall) {
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                boolean border = x == x1 - 1 || x == x2 + 1 || z == z1 - 1 || z == z2 + 1;
                if (border) {
                    for (int y = FY; y <= Y - 1; y++) {
                        k.set(x, y, z, wall == null ? brick(x, y, z) : s(wall));
                    }
                } else {
                    for (int y = FY + 1; y <= Y - 1; y++) {
                        k.set(x, y, z, AIR);
                    }
                    k.set(x, FY, z, s(((x + z) & 1) == 0 ? floorA : floorB));
                }
            }
        }
    }

    private void carve(int x1, int z1, int x2, int z2) {
        k.fill(x1, FY + 1, z1, x2, Y - 2, z2, AIR);
    }

    private void chest(int x, int z, Direction dir, LootFiller.Kind kind) {
        k.set(x, FY + 1, z, facing(Blocks.CHEST, dir));
        LootFiller.fill(level, x, FY + 1, z, rnd, kind);
    }

    private void barrel(int x, int y, int z, Direction dir, LootFiller.Kind kind) {
        k.set(x, y, z, s(Blocks.BARREL).setValue(BarrelBlock.FACING, dir));
        if (kind != null) {
            LootFiller.fill(level, x, y, z, rnd, kind);
        }
    }

    private void hang(int x, int z, boolean soul) {
        BlockState lantern = (soul ? s(Blocks.SOUL_LANTERN) : s(Blocks.LANTERN)).setValue(LanternBlock.HANGING, true);
        k.set(x, Y - 1, z, lantern);
    }

    private void table(int x1, int z1, int x2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                k.set(x, FY + 1, z, s(Blocks.SPRUCE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
            }
        }
    }

    private void chair(int x, int z, Direction look) {
        k.set(x, FY + 1, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, look.getOpposite(), false));
    }

    // ================= оболочка помещений =================
    private void secretRoomShell() {
        room(203, 204, 211, 210, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS);
    }

    private void rooms() {
        // коридор вдоль всего подвала
        room(164, 225, 266, 227, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, null);
        // камеры тюрьмы по обе стороны коридора
        for (int n = 0; n < 4; n++) {
            int cx1 = 165 + 6 * n;
            room(cx1, 218, cx1 + 4, 223, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, null);
            room(cx1, 229, cx1 + 4, 234, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, null);
        }
        room(192, 212, 222, 223, Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, null);       // винный погреб
        room(226, 212, 264, 223, Blocks.POLISHED_ANDESITE, Blocks.SMOOTH_STONE, null);      // кухня
        room(192, 229, 222, 243, Blocks.COBBLESTONE, Blocks.STONE, null);                   // склад
        room(226, 229, 264, 243, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, null);    // алхимический погреб
        // двери из коридора в помещения
        carve(206, 224, 208, 224);
        carve(243, 224, 245, 224);
        carve(206, 228, 208, 228);
        carve(243, 228, 245, 228);
        for (int x : new int[] {207, 244}) {
            k.set(x, Y - 2, 224, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true));
            k.set(x, Y - 2, 228, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
        }
    }

    // ================= тюрьма =================
    private void prison() {
        for (int n = 0; n < 4; n++) {
            int x1 = 165 + 6 * n;
            int x2 = x1 + 4;
            int mid = x1 + 2;
            // решётки в стенах к коридору, в каждой по проёму посередине
            for (int x = x1; x <= x2; x++) {
                for (int y = FY + 1; y <= FY + 3; y++) {
                    if (x != mid) {
                        k.set(x, y, 224, s(Blocks.IRON_BARS));
                        k.set(x, y, 228, s(Blocks.IRON_BARS));
                    } else {
                        k.set(x, y, 224, AIR);
                        k.set(x, y, 228, AIR);
                    }
                }
            }
            // северная камера: лежанка у задней стены, ведро, цепи, паутина
            cell(x1, 218, 223, true, n);
            cell(x1, 229, 234, false, n);
        }
        // стол стражи в конце коридора
        table(165, 225, 166, 225);
        chair(165, 226, Direction.NORTH);
        k.set(166, FY + 2, 225, s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
    }

    private void cell(int x1, int zA, int zB, boolean north, int n) {
        int back = north ? zA : zB;          // задняя стена камеры
        int front = north ? zB : zA;         // сторона коридора
        int dz = north ? 1 : -1;             // от задней стены к проёму
        k.set(x1 + 1, FY + 1, back, s(Blocks.HAY_BLOCK));
        k.set(x1 + 2, FY + 1, back, s(Blocks.HAY_BLOCK));
        k.set(x1 + 1, FY + 2, back, s(Blocks.BROWN_CARPET));
        k.set(x1 + 2, FY + 2, back, s(Blocks.BROWN_CARPET));
        k.set(x1 + 4, FY + 1, back, s(Blocks.CAULDRON));
        for (int y = Y - 1; y >= Y - 3; y--) {
            k.set(x1 + 3, y, back, s(Blocks.CHAIN));
        }
        k.set(x1, Y - 1, back, s(Blocks.COBWEB));
        if ((n + (north ? 0 : 1)) % 2 == 0) {
            hang(x1 + 2, back + 2 * dz, true);
        }
        if (n % 3 == 1) {
            k.set(x1 + 4, FY + 1, back + 2 * dz, s(Blocks.SKELETON_SKULL));
        }
        if (n == 3) {
            k.set(x1 + 4, FY + 1, front - dz, facing(Blocks.CHEST, north ? Direction.SOUTH : Direction.NORTH));
            LootFiller.fill(level, x1 + 4, FY + 1, front - dz, rnd, LootFiller.Kind.RUIN);
        }
    }

    // ================= винный погреб =================
    private void cellar() {
        for (int z = 212; z <= 223; z++) {
            barrel(192, FY + 1, z, Direction.EAST, LootFiller.Kind.KITCHEN);
            barrel(192, FY + 2, z, Direction.EAST, null);
            barrel(222, FY + 1, z, Direction.WEST, LootFiller.Kind.KITCHEN);
            barrel(222, FY + 2, z, Direction.WEST, null);
        }
        // стеллажи-бочки рядами с проходами
        for (int row : new int[] {215, 219}) {
            for (int x = 196; x <= 218; x++) {
                if (x % 4 == 3) {
                    continue;
                }
                barrel(x, FY + 1, row, Direction.SOUTH, LootFiller.Kind.KITCHEN);
                if (x % 3 == 0) {
                    barrel(x, FY + 2, row, Direction.SOUTH, null);
                } else {
                    k.set(x, FY + 2, row, s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
                }
            }
        }
        // бочки у северной стены (кроме места тайного хода x=207)
        for (int x = 194; x <= 220; x += 2) {
            if (x >= 206 && x <= 208) {
                continue;
            }
            barrel(x, FY + 1, 212, Direction.SOUTH, LootFiller.Kind.KITCHEN);
        }
        chest(221, 213, Direction.WEST, LootFiller.Kind.TREASURE);
        // подсказка: сухие трещины в стене и синий фонарь над ними
        k.fill(207, FY + 1, 211, 207, FY + 2, 211, s(Blocks.CRACKED_STONE_BRICKS));
        hang(207, 212, true);
        k.set(206, FY + 1, 212, s(Blocks.COBWEB));
        k.set(208, FY + 1, 212, s(Blocks.COBWEB));
    }

    // ================= тайная комната =================
    private void secretRoom() {
        for (int z = 205; z <= 209; z++) {
            k.fill(203, FY + 1, z, 203, FY + 3, z, s(Blocks.BOOKSHELF));
            k.fill(211, FY + 1, z, 211, FY + 3, z, s(Blocks.BOOKSHELF));
        }
        for (int z = 206; z <= 210; z++) {
            k.set(207, FY + 1, z, s(Blocks.RED_CARPET));
        }
        k.set(207, FY + 1, 205, s(Blocks.GOLD_BLOCK));
        k.set(207, FY + 2, 205, s(Blocks.WITHER_SKELETON_SKULL));
        chest(204, 205, Direction.SOUTH, LootFiller.Kind.TREASURE);
        chest(210, 205, Direction.SOUTH, LootFiller.Kind.TREASURE);
        chest(204, 209, Direction.EAST, LootFiller.Kind.TREASURE);
        chest(210, 209, Direction.WEST, LootFiller.Kind.RUIN);
        k.set(205, FY + 1, 207, s(Blocks.ENCHANTING_TABLE));
        k.set(209, FY + 1, 207, s(Blocks.LECTERN));
        for (int[] c : new int[][] {{205, 206}, {209, 206}, {206, 209}, {208, 209}}) {
            hang(c[0], c[1], true);
        }
        for (int dx : new int[] {-1, 1}) {
            k.set(207 + dx, FY + 1, 205, s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
        }
    }

    // ================= кухня =================
    private void kitchen() {
        for (int x = 228; x <= 236; x += 2) {
            k.set(x, FY + 1, 212, facing(Blocks.FURNACE, Direction.SOUTH));
        }
        k.set(238, FY + 1, 212, facing(Blocks.SMOKER, Direction.SOUTH));
        k.set(240, FY + 1, 212, facing(Blocks.SMOKER, Direction.SOUTH));
        k.set(242, FY + 1, 212, facing(Blocks.BLAST_FURNACE, Direction.SOUTH));
        for (int x = 246; x <= 252; x += 2) {
            k.set(x, FY + 1, 212, s(Blocks.CAMPFIRE));
        }
        for (int[] t : new int[][] {{232, 238}, {244, 250}}) {
            table(t[0], 216, t[1], 217);
            for (int x = t[0]; x <= t[1]; x += 2) {
                chair(x, 215, Direction.SOUTH);
                chair(x, 218, Direction.NORTH);
            }
            k.set(t[0] + 1, FY + 2, 216, s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
        }
        for (int z = 213; z <= 221; z += 2) {
            barrel(226, FY + 1, z, Direction.EAST, LootFiller.Kind.KITCHEN);
            barrel(226, FY + 2, z, Direction.EAST, null);
        }
        k.set(227, FY + 1, 222, s(Blocks.HAY_BLOCK));
        k.set(227, FY + 1, 223, s(Blocks.HAY_BLOCK));
        k.set(240, FY + 1, 222, s(Blocks.COMPOSTER));
        for (int x = 242; x <= 244; x++) {
            k.set(x, FY + 1, 222, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        }
        chest(258, 213, Direction.SOUTH, LootFiller.Kind.KITCHEN);
        chest(260, 213, Direction.SOUTH, LootFiller.Kind.KITCHEN);
        for (int x : new int[] {252, 256}) {
            k.set(x, FY + 1, 222, s(Blocks.BARREL));
            LootFiller.fill(level, x, FY + 1, 222, rnd, LootFiller.Kind.KITCHEN);
        }
    }

    // ================= склад =================
    private void storage() {
        for (int z = 230; z <= 242; z += 2) {
            barrel(192, FY + 1, z, Direction.EAST, LootFiller.Kind.HOUSE);
            barrel(192, FY + 2, z, Direction.EAST, null);
        }
        for (int[] c : new int[][] {{221, 230}, {221, 235}, {221, 240}}) {
            chest(c[0], c[1], Direction.WEST, LootFiller.Kind.CAMP);
        }
        for (int x = 194; x <= 200; x += 2) {
            for (int z : new int[] {233, 237}) {
                barrel(x, FY + 1, z, Direction.UP, LootFiller.Kind.HOUSE);
            }
        }
        for (int x = 212; x <= 218; x += 3) {
            for (int z : new int[] {232, 238}) {
                k.set(x, FY + 1, z, s(Blocks.HAY_BLOCK));
                k.set(x, FY + 2, z, s(Blocks.HAY_BLOCK));
            }
        }
        k.set(195, FY + 1, 241, s(Blocks.GRINDSTONE));
        k.set(197, FY + 1, 241, s(Blocks.ANVIL));
        k.set(215, FY + 1, 242, s(Blocks.CRAFTING_TABLE));
        chest(217, 242, Direction.NORTH, LootFiller.Kind.CAMP);
    }

    // ================= алхимический погреб =================
    private void alchemy() {
        for (int x : new int[] {230, 232, 234}) {
            k.set(x, FY + 1, 229, s(Blocks.BREWING_STAND));
        }
        for (int x : new int[] {237, 239}) {
            k.set(x, FY + 1, 229, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        }
        for (int z = 230; z <= 242; z++) {
            k.fill(264, FY + 1, z, 264, FY + 3, z, s(Blocks.BOOKSHELF));
        }
        k.set(245, FY + 1, 236, s(Blocks.ENCHANTING_TABLE));
        for (int[] b : new int[][] {{243, 234}, {247, 234}, {243, 238}, {247, 238}}) {
            k.fill(b[0], FY + 1, b[1], b[0], FY + 2, b[1], s(Blocks.BOOKSHELF));
        }
        k.set(255, FY + 1, 232, s(Blocks.LECTERN));
        k.set(257, FY + 1, 232, s(Blocks.CARTOGRAPHY_TABLE));
        chest(228, 241, Direction.NORTH, LootFiller.Kind.WORKSHOP);
        chest(260, 241, Direction.NORTH, LootFiller.Kind.WORKSHOP);
        k.set(230, FY + 1, 241, s(Blocks.BARREL));
        LootFiller.fill(level, 230, FY + 1, 241, rnd, LootFiller.Kind.WORKSHOP);
        for (int[] c : new int[][] {{232, 232}, {242, 232}, {252, 232}, {232, 240}, {252, 240}, {245, 236}}) {
            hang(c[0], c[1], true);
        }
        table(236, 241, 241, 241);
        k.set(238, FY + 2, 241, s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
    }

    // ================= вход из двора =================
    private void entrance() {
        // спуск у южной стены корпуса: ступени вниз на север, потом туннель под стеной в склад
        for (int i = 0; i <= 5; i++) {
            int z = 258 - i;
            for (int x = 203; x <= 205; x++) {
                int top = i <= 2 ? Y : Y - 1;
                k.fill(x, Y - i + 1, z, x, top, z, AIR);
                k.set(x, Y - i, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
            }
        }
        for (int z = 244; z <= 252; z++) {
            for (int x = 203; x <= 205; x++) {
                k.fill(x, FY + 1, z, x, Y - 1, z, AIR);
                k.set(x, FY, z, s(Blocks.STONE_BRICKS));
            }
            for (int x : new int[] {202, 206}) {
                for (int y = FY; y <= Y - 1; y++) {
                    k.set(x, y, z, brick(x, y, z));
                }
            }
        }
        // боковые стены спуска и фонари
        for (int z = 253; z <= 258; z++) {
            for (int x : new int[] {202, 206}) {
                k.fill(x, Y - 6, z, x, Y, z, s(Blocks.STONE_BRICKS));
            }
        }
        for (int x : new int[] {202, 206}) {
            k.fill(x, Y + 1, 256, x, Y + 2, 258, s(Blocks.STONE_BRICK_WALL));
            k.set(x, Y + 3, 257, s(Blocks.LANTERN));
        }
        hang(204, 249, false);
        hang(204, 246, false);
    }

    // ================= лестница из кухни в пиршественный зал =================
    private void stairsToHall() {
        for (int i = 1; i <= 6; i++) {
            int z = 224 - i;
            for (int x = 262; x <= 264; x++) {
                k.fill(x, FY + 1, z, x, Y - 6 + i - 1, z, s(Blocks.STONE_BRICKS));
                k.set(x, Y - 6 + i, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
            }
        }
        for (int x = 262; x <= 264; x++) {
            for (int z = 219; z <= 220; z++) {
                k.set(x, Y, z, AIR);              // проём в полу над ступенями
                k.set(x, Y + 1, z, AIR);
            }
            k.set(x, Y + 1, 218, AIR);            // ковёр с верхней ступени
            k.set(x, Y + 1, 221, s(Blocks.IRON_BARS));
        }
        for (int z = 219; z <= 220; z++) {
            k.set(261, Y + 1, z, s(Blocks.IRON_BARS));
            k.set(265, Y + 1, z, s(Blocks.IRON_BARS));
        }
    }

    // ================= свет =================
    private void lights() {
        for (int x = 170; x <= 260; x += 6) {
            hang(x, 226, false);
        }
        hang(166, 226, false);
        for (int x : new int[] {198, 206, 214}) {
            for (int z : new int[] {217, 221}) {
                hang(x, z, false);
            }
        }
        for (int x : new int[] {232, 242, 252, 260}) {
            for (int z : new int[] {214, 220}) {
                hang(x, z, false);
            }
        }
        for (int x : new int[] {198, 206, 216}) {
            for (int z : new int[] {232, 238}) {
                hang(x, z, false);
            }
        }
    }
}
