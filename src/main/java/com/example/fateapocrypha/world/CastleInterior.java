package com.example.fateapocrypha.world;

import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Обстановка замка поверх построенной оболочки: тронный зал во дворце на крыше, четыре мастерские во дворе
 * (кузница, оружейная, кухня, мастерская мага) и сундуки с ресурсами в библиотеке, пиршественном зале и спальнях.
 * Запускается последним шагом CastleBuilder, всё в мировых координатах.
 */
final class CastleInterior {
    private static final int Y = CastleSite.Y;
    private static final int X0 = CastleSite.X0;
    private static final int X1 = CastleSite.X0 + CastleSite.W;
    private static final int CX = CastleSite.CX;

    private final ServerLevel level;
    private final BuildKit k;
    private final Random rnd = new Random(1305L);

    private CastleInterior(ServerLevel level) {
        this.level = level;
        this.k = new BuildKit(level);
    }

    static void build(ServerLevel level) {
        CastleInterior in = new CastleInterior(level);
        in.throneRoom();
        in.workshops();
        in.wingsLoot();
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState facing(Block block, Direction dir) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir);
    }

    private void chest(int x, int y, int z, Direction dir, LootFiller.Kind kind) {
        k.set(x, y, z, facing(Blocks.CHEST, dir));
        LootFiller.fill(level, x, y, z, rnd, kind);
    }

    private void barrel(int x, int y, int z, LootFiller.Kind kind) {
        k.set(x, y, z, s(Blocks.BARREL));
        LootFiller.fill(level, x, y, z, rnd, kind);
    }

    // ================= тронный зал (дворец на крыше корпуса) =================
    private void throneRoom() {
        int base = Y + 24;           // уровень пола дворца
        int z1 = 184;                // северная стена зала (внутри)
        int z2 = 206;                // южная стена, у двери с террасы
        // дорожка от двери к трону: красная с синей каймой
        for (int z = 190; z <= z2; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                k.set(CX + dx, base, z, s(Blocks.RED_CARPET));
            }
            k.set(CX - 2, base, z, s(Blocks.BLUE_CARPET));
            k.set(CX + 2, base, z, s(Blocks.BLUE_CARPET));
        }
        // помост в два яруса: нижний широкий, верхний под трон
        k.fill(CX - 8, base, z1, CX + 8, base, 189, s(Blocks.SMOOTH_QUARTZ));
        for (int x = CX - 8; x <= CX + 8; x++) {
            k.set(x, base, 190, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
        }
        k.fill(CX - 5, base + 1, z1, CX + 5, base + 1, 186, s(Blocks.SMOOTH_QUARTZ));
        for (int x = CX - 5; x <= CX + 5; x++) {
            k.set(x, base + 1, 187, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
        }
        k.fill(CX - 8, base, 189, CX + 8, base, 189, s(Blocks.GOLD_BLOCK));   // золотая кайма под нижней ступенью
        // трон: сиденье, подлокотники и высокая спинка из резного кварца с золотой короной
        k.set(CX, base + 2, 185, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
        k.set(CX - 1, base + 2, 185, s(Blocks.QUARTZ_SLAB));
        k.set(CX + 1, base + 2, 185, s(Blocks.QUARTZ_SLAB));
        k.fill(CX - 1, base + 2, z1, CX + 1, base + 7, z1, s(Blocks.CHISELED_QUARTZ_BLOCK));
        k.fill(CX, base + 8, z1, CX, base + 8, z1, s(Blocks.GOLD_BLOCK));
        k.set(CX - 1, base + 8, z1, s(Blocks.GOLD_BLOCK));
        k.set(CX + 1, base + 8, z1, s(Blocks.GOLD_BLOCK));
        // знамёна на северной стене
        for (int dx : new int[] {-12, -8, -4, 4, 8, 12}) {
            k.set(CX + dx, base + 4, z1, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
        }
        // колонны вдоль зала: кварц, золотое основание, капитель и фонарь
        for (int x : new int[] {CX - 17, CX + 17}) {
            for (int z = 191; z <= 203; z += 6) {
                k.set(x, base, z, s(Blocks.GOLD_BLOCK));
                k.fill(x, base + 1, z, x, base + 8, z, s(Blocks.QUARTZ_PILLAR));
                k.set(x, base + 9, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
                k.set(x, base + 10, z, s(Blocks.LANTERN));
            }
        }
        // люстры под коньком крыши: цепь, золотое кольцо и фонари
        for (int x : new int[] {CX - 9, CX, CX + 9}) {
            for (int y = base + 24; y >= base + 18; y--) {
                k.set(x, y, 195, s(Blocks.CHAIN));
            }
            k.set(x, base + 17, 195, BuildKit.HANGING_LANTERN);
            for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                k.set(x + d[0], base + 18, 195 + d[1], BuildKit.HANGING_LANTERN);
            }
        }
        // сундуки с сокровищами по краям помоста
        chest(CX - 7, base + 1, 185, Direction.SOUTH, LootFiller.Kind.TREASURE);
        chest(CX + 7, base + 1, 185, Direction.SOUTH, LootFiller.Kind.TREASURE);
        // скамьи вдоль боковых стен
        for (int z = 192; z <= 200; z += 2) {
            k.set(194, base, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            k.set(236, base, z, BuildKit.stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
        }
    }

    // ================= мастерские во дворе =================
    private void workshops() {
        smithy(X0 + 8, 255, X0 + 20, 266);
        armory(X1 - 20, 255, X1 - 8, 266);
        kitchen(X0 + 8, 326, X0 + 20, 337);
        alchemy(X1 - 20, 326, X1 - 8, 337);
    }

    /** Кузница: горн, наковальни, точило, сундук и бочка с железом. */
    private void smithy(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        k.set(x1 + 3, y, z1 + 1, facing(Blocks.BLAST_FURNACE, Direction.SOUTH));
        k.set(x1 + 4, y, z1 + 1, s(Blocks.GRINDSTONE));
        k.set(x1 + 5, y, z1 + 1, s(Blocks.SMITHING_TABLE));
        k.set(x2 - 2, y, z1 + 1, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(x1 + 1, y, z1 + 4, s(Blocks.ANVIL));
        chest(x1 + 1, y, z1 + 3, Direction.EAST, LootFiller.Kind.SMITHY);
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.SMITHY);
        k.set(x2 - 1, y, z1 + 3, s(Blocks.IRON_BLOCK));
    }

    /** Оружейная: стойки с сундуками, точило, стол лучника. */
    private void armory(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        for (int dx : new int[] {4, 6, 8}) {
            chest(x1 + dx, y, z1 + 1, Direction.SOUTH, LootFiller.Kind.ARMORY);
        }
        k.set(x1 + 10, y, z1 + 1, s(Blocks.GRINDSTONE));
        k.set(x1 + 10, y, z1 + 3, s(Blocks.FLETCHING_TABLE));
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.ARMORY);
        k.fill(x1 + 2, y, z1 + 5, x1 + 4, y, z1 + 5, s(Blocks.RED_CARPET));
        k.set(x1 + 3, y, z1 + 4, s(Blocks.TARGET));
    }

    /** Кухня: коптильня, бочки с едой, сено, стол. */
    private void kitchen(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        k.set(x1 + 3, y, z1 + 1, facing(Blocks.SMOKER, Direction.SOUTH));
        k.set(x1 + 4, y, z1 + 1, s(Blocks.COMPOSTER));
        barrel(x1 + 6, y, z1 + 1, LootFiller.Kind.KITCHEN);
        barrel(x1 + 8, y, z1 + 1, LootFiller.Kind.KITCHEN);
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.KITCHEN);
        chest(x2 - 1, y, z1 + 4, Direction.WEST, LootFiller.Kind.KITCHEN);
        k.set(x1 + 1, y, z1 + 5, s(Blocks.HAY_BLOCK));
        k.set(x1 + 1, y, z1 + 6, s(Blocks.HAY_BLOCK));
        for (int dx = 4; dx <= 6; dx++) {
            k.set(x1 + dx, y, z1 + 5, s(Blocks.SPRUCE_FENCE));
            k.set(x1 + dx, y + 1, z1 + 5, s(Blocks.SPRUCE_PRESSURE_PLATE));
        }
    }

    /** Мастерская мага: стойки зельеварения, стол зачарований, книжные полки. */
    private void alchemy(int x1, int z1, int x2, int z2) {
        int y = Y + 1;
        k.set(x1 + 3, y, z1 + 1, s(Blocks.BREWING_STAND));
        k.set(x1 + 4, y, z1 + 1, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        k.set(x1 + 5, y, z1 + 1, s(Blocks.BREWING_STAND));
        barrel(x2 - 1, y, z1 + 1, LootFiller.Kind.WORKSHOP);
        chest(x1 + 1, y, z1 + 4, Direction.EAST, LootFiller.Kind.WORKSHOP);
        for (int z = z1 + 3; z <= z1 + 8; z++) {
            k.fill(x2 - 1, y, z, x2 - 1, y + 2, z, s(Blocks.BOOKSHELF));
        }
        k.set(x2 - 3, y, z1 + 5, s(Blocks.ENCHANTING_TABLE));
        k.set(x1 + 1, y, z1 + 7, facing(Blocks.LECTERN, Direction.EAST));
        for (int[] c : new int[][] {{x1 + 2, z1 + 8}, {x2 - 3, z1 + 8}}) {
            k.set(c[0], y, c[1], s(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
        }
    }

    // ================= ресурсы в крыльях корпуса =================
    private void wingsLoot() {
        int y2 = Y + 13;   // второй этаж
        // спальни: бочки у кроватей уже стоят (CastleBuilder), наполняем их и добавляем сундуки
        for (int z = 184; z <= 240; z += 8) {
            k.set(167, y2, z, s(Blocks.BARREL));
            LootFiller.fill(level, 167, y2, z, rnd, LootFiller.Kind.BEDROOM);
            k.set(263, y2, z, s(Blocks.BARREL));
            LootFiller.fill(level, 263, y2, z, rnd, LootFiller.Kind.BEDROOM);
        }
        for (int z = 184; z <= 224; z += 8) {
            chest(164, y2, z + 1, Direction.EAST, LootFiller.Kind.BEDROOM);
            chest(265, y2, z + 1, Direction.WEST, LootFiller.Kind.BEDROOM);
        }
        // библиотека (западное крыло)
        for (int z : new int[] {187, 203, 219}) {
            chest(164, Y + 1, z, Direction.EAST, LootFiller.Kind.LIBRARY);
        }
        // пиршественный зал (восточное крыло): бочки с припасами у стены
        for (int z : new int[] {190, 202, 214}) {
            barrel(266, Y + 1, z, LootFiller.Kind.KITCHEN);
        }
    }
}
