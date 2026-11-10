package com.example.fateapocrypha.scene;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Доработка интерьеров замка Иггдмилления: лаборатория (локация 1) и коридор падения (локация 2).
 * Всё ставится только в свободное место или заменяет голые стены и пол; зоны сцен (проходы игрока и камеры) остаются свободными.
 */
public final class StoryInteriors {
    private static final int Y0 = StoryWorld.Y0;

    private StoryInteriors() {
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static boolean isAir(ServerLevel l, int x, int y, int z) {
        return l.getBlockState(Reloc.pos(x, y, z)).isAir();
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState state) {
        l.setBlock(Reloc.pos(x, y, z), state, 2);
    }

    /** Ставит блок только в свободный воздух. */
    private static void put(ServerLevel l, int x, int y, int z, BlockState state) {
        if (isAir(l, x, y, z)) {
            set(l, x, y, z, state);
        }
    }

    private static void wallBanner(ServerLevel l, int x, int y, int z, Block banner, Direction facing) {
        put(l, x, y, z, banner.defaultBlockState().setValue(WallBannerBlock.FACING, facing));
    }

    public static void build(ServerLevel l) {
        lab(l);
        corridor(l);
    }

    // ================= Локация 1: лаборатория =================
    private static void lab(ServerLevel l) {
        // катушки Тесла по углам помоста главной капсулы
        for (int[] c : new int[][] {{111, 111}, {119, 111}, {111, 119}, {119, 119}}) {
            if (isAir(l, c[0], Y0 + 1, c[1])) {
                set(l, c[0], Y0 + 1, c[1], s(Blocks.WAXED_COPPER_BLOCK));
                for (int y = Y0 + 2; y <= Y0 + 4; y++) {
                    set(l, c[0], y, c[1], s(Blocks.LIGHTNING_ROD));
                }
                put(l, c[0], Y0 + 5, c[1], s(Blocks.SEA_LANTERN));
            }
        }

        // лучи на полу: восемь лучей от внешнего кольца к стенам и внешнее кольцо
        for (int a = 0; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            for (int r = 9; r <= 14; r++) {
                int x = 115 + (int) Math.round(r * Math.cos(rad));
                int z = 115 + (int) Math.round(r * Math.sin(rad));
                if (x < 101 || x > 129 || z < 101 || z > 129 || !isAir(l, x, Y0 + 1, z)) {
                    continue;
                }
                set(l, x, Y0, z, r % 2 == 0 ? s(Blocks.POLISHED_BLACKSTONE_BRICKS) : s(Blocks.LIME_CONCRETE));
            }
        }
        for (int x = 101; x <= 129; x++) {
            for (int z = 101; z <= 129; z++) {
                double d = Math.hypot(x - 115, z - 115);
                if (d >= 12.0D && d < 12.9D && isAir(l, x, Y0 + 1, z)) {
                    set(l, x, Y0, z, s(Blocks.WHITE_CONCRETE));
                }
            }
        }

        // кольцо подвесных световых стержней под потолком над капсулой
        for (int k = 0; k < 24; k++) {
            double rad = Math.toRadians(k * 15.0D);
            int x = 115 + (int) Math.round(5.0D * Math.cos(rad));
            int z = 115 + (int) Math.round(5.0D * Math.sin(rad));
            put(l, x, Y0 + 8, z, s(Blocks.END_ROD).setValue(EndRodBlock.FACING, Direction.DOWN));
        }

        // знамёна Иггдмилления на стенах зала
        for (int x : new int[] {107, 115, 123}) {
            wallBanner(l, x, Y0 + 5, 101, Blocks.BLACK_WALL_BANNER, Direction.SOUTH);
            wallBanner(l, x, Y0 + 5, 129, Blocks.BLACK_WALL_BANNER, Direction.NORTH);
        }
        for (int z : new int[] {107, 115, 123}) {
            wallBanner(l, 101, Y0 + 7, z, Blocks.GREEN_WALL_BANNER, Direction.EAST);
            wallBanner(l, 129, Y0 + 7, z, Blocks.GREEN_WALL_BANNER, Direction.WEST);
        }

        // длинный коридор: медные трубы вдоль стен
        for (int x = 132; x <= 168; x++) {
            put(l, x, Y0 + 4, 114, s(Blocks.EXPOSED_COPPER));
            put(l, x, Y0 + 4, 116, s(Blocks.EXPOSED_COPPER));
        }

        // зал выхода: арка у рамы выхода, кольцо на полу, знамёна
        for (int z : new int[] {112, 118}) {
            for (int y = Y0 + 1; y <= Y0 + 6; y++) {
                set(l, 189, y, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            }
        }
        for (int z = 112; z <= 118; z++) {
            set(l, 189, Y0 + 7, z, s(Blocks.CHISELED_POLISHED_BLACKSTONE));
        }
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a);
            for (double r : new double[] {3.2D, 4.2D}) {
                int x = 180 + (int) Math.round(r * Math.cos(rad));
                int z = 115 + (int) Math.round(r * Math.sin(rad));
                if (isAir(l, x, Y0 + 1, z)) {
                    set(l, x, Y0, z, r < 4.0D ? s(Blocks.WHITE_CONCRETE) : s(Blocks.LIME_CONCRETE));
                }
            }
        }
        for (int x : new int[] {175, 180, 185}) {
            wallBanner(l, x, Y0 + 5, 106, Blocks.BLACK_WALL_BANNER, Direction.SOUTH);
            wallBanner(l, x, Y0 + 5, 124, Blocks.BLACK_WALL_BANNER, Direction.NORTH);
        }
    }

    // ================= Локация 2: коридор падения =================
    private static void corridor(ServerLevel l) {
        // стрельчатые витражные окна между колоннами (3 в ширину, остриё сверху)
        Block[] glass = {Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS,
                Blocks.BLUE_STAINED_GLASS};
        int[] centers = {108, 113, 118, 123};
        for (int i = 0; i < centers.length; i++) {
            int cx = centers[i];
            for (int z : new int[] {298, 306}) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int y = Y0 + 2; y <= Y0 + 4; y++) {
                        set(l, cx + dx, y, z, s(glass[(i + Math.abs(dx)) % glass.length]));
                    }
                    set(l, cx + dx, Y0 + 1, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
                }
                set(l, cx, Y0 + 5, z, s(glass[i % glass.length]));
                set(l, cx, Y0 + 6, z, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
            }
        }

        // ребра свода между колоннами и знамёна на колоннах
        for (int x = 105; x <= 125; x += 5) {
            for (int z = 300; z <= 304; z++) {
                put(l, x, Y0 + 6, z, s(Blocks.DEEPSLATE_BRICK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
            }
            wallBanner(l, x, Y0 + 4, 300, Blocks.PURPLE_WALL_BANNER, Direction.SOUTH);
            wallBanner(l, x, Y0 + 4, 304, Blocks.PURPLE_WALL_BANNER, Direction.NORTH);
        }

        // ковровая дорожка с красной каймой (центральная чёрная полоса уже есть)
        for (int x = 101; x <= 129; x++) {
            put(l, x, Y0 + 1, 301, s(Blocks.RED_CARPET));
            put(l, x, Y0 + 1, 303, s(Blocks.RED_CARPET));
        }
        for (int x = 104; x <= 126; x += 11) {
            set(l, x, Y0, 302, s(Blocks.GOLD_BLOCK));
        }

        // люстры над дорожкой: цепь и подвесной фонарь
        for (int x : new int[] {108, 113, 118, 123}) {
            for (int y = Y0 + 5; y <= Y0 + 6; y++) {
                put(l, x, y, 302, s(Blocks.CHAIN));
            }
            put(l, x, Y0 + 4, 302, s(Blocks.LANTERN).setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }

        // растения и свечи по углам
        put(l, 101, Y0 + 1, 299, s(Blocks.POTTED_FERN));
        put(l, 129, Y0 + 1, 305, s(Blocks.POTTED_FERN));
        put(l, 101, Y0 + 1, 305, s(Blocks.POTTED_DEAD_BUSH));
        put(l, 129, Y0 + 1, 299, s(Blocks.POTTED_DEAD_BUSH));
        for (int x : new int[] {104, 111, 119, 126}) {
            put(l, x, Y0 + 1, 299, s(Blocks.CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
            put(l, x, Y0 + 1, 305, s(Blocks.CANDLE).setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        }
    }
}
