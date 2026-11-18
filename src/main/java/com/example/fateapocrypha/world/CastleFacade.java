package com.example.fateapocrypha.world;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Архитектурная отделка замка по образцу больших построек: рельеф фасада вместо плоских стен (карнизы, пояса, пилястры),
 * шпили и пинакли на крыше, тяжёлые башни с зелёной медной кровлей, парадный портик с колоннами и фронтоном над входом,
 * центральная часовая башня со ступенчатой вышкой и шпилем, контрфорсы на внешней стене, формальные сады
 * с живыми изгородями, деревьями и клумбами. Работает поверх готовой оболочки и ничего не ломает: ставит блоки
 * только в воздух, кроме мест, которые заведомо принадлежат отделке.
 */
final class CastleFacade {
    private static final int Y = CastleSite.Y;
    private static final int CX = CastleSite.CX;
    private static final int KX1 = CastleSite.KX1;
    private static final int KX2 = CastleSite.KX2;
    private static final int KZ1 = CastleSite.KZ1;
    private static final int KZ2 = CastleSite.KZ2;
    private static final int X0 = CastleSite.X0;
    private static final int Z0 = CastleSite.Z0;
    private static final int X1 = CastleSite.X0 + CastleSite.W;
    private static final int Z1 = CastleSite.Z0 + CastleSite.D;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final ServerLevel level;
    private final BuildKit k;

    private CastleFacade(ServerLevel level) {
        this.level = level;
        this.k = new BuildKit(level);
    }

    static void build(ServerLevel level) {
        CastleFacade f = new CastleFacade(level);
        f.cornices();
        f.pinnacles();
        f.tallWindows();
        f.portico();
        f.clockTower();
        f.wallButtresses();
        f.gardens();
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private void soft(int x, int y, int z, BlockState state) {
        if (k.isAir(x, y, z)) {
            k.set(x, y, z, state);
        }
    }

    private static BlockState stair(Block b, Direction facing, boolean top) {
        return BuildKit.stairs(b, facing, top);
    }

    // ================= карнизы и пояса =================
    private void cornices() {
        for (int y : new int[] {Y + 12, Y + 21}) {
            for (int x = KX1; x <= KX2; x++) {
                soft(x, y, KZ2 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true));
                soft(x, y, KZ1 - 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true));
            }
            for (int z = KZ1; z <= KZ2; z++) {
                soft(KX1 - 1, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
                soft(KX2 + 1, y, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
            }
        }
        // выступающая плита под террасой крыши
        for (int x = KX1; x <= KX2; x++) {
            soft(x, Y + 22, KZ2 + 1, s(Blocks.STONE_BRICK_SLAB));
            soft(x, Y + 22, KZ1 - 1, s(Blocks.STONE_BRICK_SLAB));
        }
        for (int z = KZ1; z <= KZ2; z++) {
            soft(KX1 - 1, Y + 22, z, s(Blocks.STONE_BRICK_SLAB));
            soft(KX2 + 1, Y + 22, z, s(Blocks.STONE_BRICK_SLAB));
        }
    }

    // ================= пинакли по краю крыши =================
    private void pinnacle(int x, int z) {
        k.fill(x, Y + 24, z, x, Y + 27, z, s(Blocks.STONE_BRICKS));
        k.set(x, Y + 25, z, s(Blocks.CHISELED_STONE_BRICKS));
        k.fill(x, Y + 28, z, x, Y + 29, z, s(Blocks.STONE_BRICK_WALL));
        k.set(x, Y + 30, z, s(Blocks.LIGHTNING_ROD));
        for (Direction d : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            soft(x + d.getStepX(), Y + 24, z + d.getStepZ(), stair(Blocks.STONE_BRICK_STAIRS, d.getOpposite(), false));
        }
    }

    private void pinnacles() {
        for (int x = KX1 + 12; x <= KX2 - 12; x += 8) {
            pinnacle(x, KZ2);
            pinnacle(x, KZ1);
        }
        for (int z = KZ1 + 12; z <= KZ2 - 12; z += 8) {
            pinnacle(KX1, z);
            pinnacle(KX2, z);
        }
    }

    // ================= высокие окна южного фасада =================
    private void tallWindows() {
        for (int xc : new int[] {CX - 24, CX - 17, CX + 17, CX + 24}) {
            int glassZ = KZ2 - 1;
            k.fill(xc - 2, Y + 5, KZ2 - 3, xc + 2, Y + 13, KZ2, AIR);
            k.fill(xc - 1, Y + 14, KZ2 - 3, xc + 1, Y + 14, KZ2, AIR);
            k.set(xc, Y + 15, glassZ, AIR);
            k.fill(xc - 2, Y + 5, glassZ, xc + 2, Y + 13, glassZ, s(Blocks.GLASS));
            k.fill(xc - 1, Y + 14, glassZ, xc + 1, Y + 14, glassZ, s(Blocks.GLASS));
            k.set(xc, Y + 15, glassZ, s(Blocks.GLASS));
            k.fill(xc - 2, Y + 9, glassZ, xc + 2, Y + 9, glassZ, s(Blocks.POLISHED_ANDESITE));   // поперечный импост
            k.fill(xc, Y + 5, glassZ, xc, Y + 13, glassZ, s(Blocks.POLISHED_ANDESITE));          // мулион
            // обрамление снаружи: пилястры и стрельчатая перемычка
            for (int dx : new int[] {-3, 3}) {
                k.fill(xc + dx, Y + 1, KZ2 + 1, xc + dx, Y + 15, KZ2 + 1, s(Blocks.QUARTZ_PILLAR));
                k.set(xc + dx, Y + 16, KZ2 + 1, s(Blocks.CHISELED_QUARTZ_BLOCK));
            }
            k.set(xc - 2, Y + 15, KZ2 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
            k.set(xc + 2, Y + 15, KZ2 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
            k.set(xc - 1, Y + 16, KZ2 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
            k.set(xc + 1, Y + 16, KZ2 + 1, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
            k.set(xc, Y + 16, KZ2 + 1, s(Blocks.CHISELED_STONE_BRICKS));
            k.set(xc, Y + 17, KZ2 + 1, s(Blocks.GOLD_BLOCK));
        }
    }

    // ================= портик над главным входом =================
    private void portico() {
        int zc = KZ2 + 4;
        for (int x : new int[] {CX - 10, CX - 7, CX - 4, CX + 4, CX + 7, CX + 10}) {
            k.set(x, Y + 1, zc, s(Blocks.POLISHED_DIORITE));
            k.fill(x, Y + 2, zc, x, Y + 11, zc, s(Blocks.QUARTZ_PILLAR));
            k.set(x, Y + 12, zc, s(Blocks.CHISELED_QUARTZ_BLOCK));
        }
        // антаблемент и плоская кровля портика
        for (int x = CX - 11; x <= CX + 11; x++) {
            for (int z = KZ2 + 1; z <= zc + 1; z++) {
                k.set(x, Y + 13, z, s(Blocks.POLISHED_DIORITE));
            }
            k.set(x, Y + 14, zc + 1, s(Blocks.STONE_BRICK_SLAB));
        }
        // фронтон: треугольник с золотой эмблемой
        for (int r = 0; r <= 5; r++) {
            int half = 10 - 2 * r;
            for (int x = CX - half; x <= CX + half; x++) {
                k.set(x, Y + 14 + r, zc + 1, s(r == 5 ? Blocks.CHISELED_QUARTZ_BLOCK : Blocks.QUARTZ_BLOCK));
            }
        }
        k.fill(CX - 1, Y + 15, zc + 1, CX + 1, Y + 16, zc + 1, s(Blocks.GOLD_BLOCK));
        // наклонные карнизы фронтона
        for (int r = 0; r <= 5; r++) {
            int half = 10 - 2 * r;
            k.set(CX - half - 1, Y + 14 + r, zc + 1, stair(Blocks.QUARTZ_STAIRS, Direction.EAST, false));
            k.set(CX + half + 1, Y + 14 + r, zc + 1, stair(Blocks.QUARTZ_STAIRS, Direction.WEST, false));
        }
        for (int dx : new int[] {-2, 2}) {
            k.set(CX + dx, Y + 12, KZ2 + 2, BuildKit.HANGING_LANTERN);
        }
        // широкие ступени крыльца
        for (int step = 0; step < 2; step++) {
            for (int x = CX - 11 + step; x <= CX + 11 - step; x++) {
                k.set(x, Y, zc + 2 + step, s(Blocks.POLISHED_DIORITE));
            }
        }
    }

    // ================= часовая башня =================
    private void shell(int x1, int z1, int x2, int z2, int y1, int y2, Block wall) {
        k.fill(x1, y1, z1, x2, y2, z2, s(wall));
        k.fill(x1 + 1, y1, z1 + 1, x2 - 1, y2 - 1, z2 - 1, AIR);
        k.fill(x1, y2, z1, x2, y2, z2, s(Blocks.STONE_BRICKS));   // перекрытие яруса
    }

    private void pointedWindow(int x, int z, boolean alongX, int y0, int height) {
        // alongX: окно в стене z=const (ширина вдоль x), иначе в стене x=const
        for (int d = -1; d <= 1; d++) {
            int wx = alongX ? x + d : x;
            int wz = alongX ? z : z + d;
            k.fill(wx, y0, wz, wx, y0 + height - 1, wz, s(Blocks.GLASS));
        }
        int wx = x;
        int wz = z;
        k.set(wx, y0 + height, wz, s(Blocks.GLASS));
        for (int d : new int[] {-2, 2}) {
            int fx = alongX ? x + d : x;
            int fz = alongX ? z : z + d;
            k.fill(fx, y0, fz, fx, y0 + height, fz, s(Blocks.POLISHED_ANDESITE));
        }
        k.set(x, y0 + height + 1, z, s(Blocks.CHISELED_STONE_BRICKS));
    }

    private void clockTower() {
        int cz = 175;
        // ярус 1: широкое основание на северной части крыши
        shell(CX - 8, 169, CX + 8, 180, Y + 24, Y + 40, Blocks.STONE_BRICKS);
        for (int[] c : new int[][] {{CX - 8, 169}, {CX + 8, 169}, {CX - 8, 180}, {CX + 8, 180}}) {
            k.fill(c[0], Y + 24, c[1], c[0], Y + 40, c[1], s(Blocks.QUARTZ_PILLAR));
            k.set(c[0], Y + 41, c[1], s(Blocks.STONE_BRICK_WALL));
            k.set(c[0], Y + 42, c[1], s(Blocks.LIGHTNING_ROD));
        }
        for (int x = CX - 8; x <= CX + 8; x++) {
            k.set(x, Y + 30, 169, s(Blocks.POLISHED_ANDESITE));
            k.set(x, Y + 30, 180, s(Blocks.POLISHED_ANDESITE));
        }
        for (int z = 169; z <= 180; z++) {
            k.set(CX - 8, Y + 30, z, s(Blocks.POLISHED_ANDESITE));
            k.set(CX + 8, Y + 30, z, s(Blocks.POLISHED_ANDESITE));
        }
        for (int dx : new int[] {-4, 0, 4}) {
            pointedWindow(CX + dx, 180, true, Y + 32, 5);
            pointedWindow(CX + dx, 169, true, Y + 32, 5);
        }
        pointedWindow(CX - 8, 175, false, Y + 32, 5);
        pointedWindow(CX + 8, 175, false, Y + 32, 5);
        // ярус 2
        shell(CX - 5, 171, CX + 5, 178, Y + 41, Y + 54, Blocks.STONE_BRICKS);
        for (int[] c : new int[][] {{CX - 5, 171}, {CX + 5, 171}, {CX - 5, 178}, {CX + 5, 178}}) {
            k.fill(c[0], Y + 41, c[1], c[0], Y + 54, c[1], s(Blocks.QUARTZ_PILLAR));
        }
        // циферблаты на четырёх гранях: белый бетон, чёрные метки и стрелки
        clockFace(CX, 178, true);
        clockFace(CX, 171, true);
        clockFace(CX - 5, cz, false);
        clockFace(CX + 5, cz, false);
        // ярус 3: открытая звонница на четырёх колоннах
        for (int[] c : new int[][] {{CX - 3, 172}, {CX + 3, 172}, {CX - 3, 178}, {CX + 3, 178}}) {
            k.fill(c[0], Y + 55, c[1], c[0], Y + 66, c[1], s(Blocks.QUARTZ_PILLAR));
            k.set(c[0], Y + 67, c[1], s(Blocks.CHISELED_QUARTZ_BLOCK));
        }
        k.fill(CX - 3, Y + 67, 172, CX + 3, Y + 67, 178, s(Blocks.POLISHED_DIORITE));
        for (int x = CX - 2; x <= CX + 2; x++) {
            soft(x, Y + 55, 172, s(Blocks.IRON_BARS));
            soft(x, Y + 55, 178, s(Blocks.IRON_BARS));
        }
        for (int z = 173; z <= 177; z++) {
            soft(CX - 3, Y + 55, z, s(Blocks.IRON_BARS));
            soft(CX + 3, Y + 55, z, s(Blocks.IRON_BARS));
        }
        k.set(CX, Y + 66, cz, BuildKit.HANGING_LANTERN);
        // шпиль: зелёная медь, сужается до острия
        for (int i = 0; i <= 12; i++) {
            int half = 3 - i / 4;
            Block copper = i % 3 == 0 ? Blocks.WEATHERED_COPPER : Blocks.OXIDIZED_COPPER;
            k.fill(CX - half, Y + 68 + i, cz - half, CX + half, Y + 68 + i, cz + half, s(copper));
        }
        k.set(CX, Y + 81, cz, s(Blocks.GOLD_BLOCK));
        k.fill(CX, Y + 82, cz, CX, Y + 84, cz, s(Blocks.LIGHTNING_ROD));
    }

    private void clockFace(int fixed, int along, boolean alongX) {
        // alongX: циферблат на грани z=along (ширина по x вокруг fixed); иначе на грани x=fixed (ширина по z вокруг along)
        int y0 = Y + 47;
        for (int a = -2; a <= 2; a++) {
            for (int b = 0; b <= 4; b++) {
                int x = alongX ? fixed + a : fixed;
                int z = alongX ? along : along + a;
                k.set(x, y0 + b, z, s(Blocks.WHITE_CONCRETE));
            }
        }
        BlockState dark = s(Blocks.BLACK_CONCRETE);
        int[][] marks = {{0, 4}, {2, 2}, {0, 0}, {-2, 2}};
        for (int[] m : marks) {
            k.set(alongX ? fixed + m[0] : fixed, y0 + m[1], alongX ? along : along + m[0], dark);
        }
        for (int[] h : new int[][] {{0, 2}, {0, 3}, {1, 2}}) {   // центр, минутная вверх, часовая вправо
            k.set(alongX ? fixed + h[0] : fixed, y0 + h[1], alongX ? along : along + h[0], dark);
        }
    }

    // ================= контрфорсы внешней стены =================
    private void wallButtress(int x, int z, Direction out) {
        // out: куда выступает контрфорс от стены; ширина 3 вдоль стены, глубина 2
        boolean ns = out == Direction.NORTH || out == Direction.SOUTH;
        for (int l = -1; l <= 1; l++) {
            for (int d = 1; d <= 2; d++) {
                int bx = x + (ns ? l : out.getStepX() * d);
                int bz = z + (ns ? out.getStepZ() * d : l);
                int top = d == 1 ? Y + 13 : Y + 8;
                int bottom = Math.min(Y, BuildKit.ground(bx, bz));
                k.fill(bx, bottom, bz, bx, top, bz, s(Blocks.STONE_BRICKS));
                if (d == 2) {
                    k.set(bx, Y + 9, bz, stair(Blocks.STONE_BRICK_STAIRS, out.getOpposite(), false));
                }
            }
        }
        int px = x + (ns ? 0 : out.getStepX());
        int pz = z + (ns ? out.getStepZ() : 0);
        k.fill(px, Y + 14, pz, px, Y + 16, pz, s(Blocks.STONE_BRICKS));
        k.set(px, Y + 17, pz, s(Blocks.STONE_BRICK_WALL));
        k.set(px, Y + 18, pz, s(Blocks.LIGHTNING_ROD));
    }

    private void wallButtresses() {
        for (int x = X0 + 18; x <= X1 - 18; x += 14) {
            wallButtress(x, Z0, Direction.NORTH);
            if (Math.abs(x - CX) > 20) {
                wallButtress(x, Z1, Direction.SOUTH);
            }
        }
        for (int z = Z0 + 18; z <= Z1 - 18; z += 14) {
            wallButtress(X0, z, Direction.WEST);
            wallButtress(X1, z, Direction.EAST);
        }
    }

    // ================= сады =================
    private static BlockState hedge() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private void tree(int x, int z) {
        k.fill(x, Y + 1, z, x, Y + 4, z, s(Blocks.OAK_LOG));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= 5) {
                        soft(x + dx, Y + 5 + dy, z + dz, hedge());
                    }
                }
            }
        }
    }

    private void parterre(int x1, int z1, int x2, int z2, int variant) {
        Block[] flowers = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.ALLIUM};
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean path = x == (x1 + x2) / 2 || z == (z1 + z2) / 2;
                if (edge) {
                    k.set(x, Y + 1, z, hedge());
                    if ((x + z) % 4 == 0) {
                        k.set(x, Y + 2, z, hedge());
                    }
                } else if (path) {
                    k.set(x, Y, z, s(Blocks.DIRT_PATH));
                } else {
                    k.set(x, Y, z, s(Blocks.GRASS_BLOCK));
                    int band = ((x - x1) / 3 + (z - z1) / 3 + variant) % flowers.length;
                    if (((x - x1) + (z - z1)) % 2 == 0) {
                        k.set(x, Y + 1, z, s(flowers[band]));
                    }
                }
            }
        }
        int cxm = (x1 + x2) / 2;
        int czm = (z1 + z2) / 2;
        k.fill(cxm, Y + 1, czm, cxm, Y + 3, czm, s(Blocks.QUARTZ_PILLAR));
        k.set(cxm, Y + 4, czm, s(Blocks.CHISELED_QUARTZ_BLOCK));
        k.set(cxm, Y + 5, czm, s(Blocks.LANTERN));
    }

    private void gardens() {
        // клумбы-партеры перед корпусом по обе стороны от главной дороги
        parterre(176, 254, 196, 262, 0);
        parterre(234, 254, 254, 262, 3);
        // аллея: живые изгороди вдоль главной дороги и деревья
        for (int z = 288; z <= 318; z++) {
            if (z >= 296 && z <= 300) {
                continue;
            }
            for (int x : new int[] {CX - 8, CX + 8}) {
                k.set(x, Y + 1, z, hedge());
                if (z % 3 == 0) {
                    k.set(x, Y + 2, z, hedge());
                }
            }
        }
        for (int[] t : new int[][] {{CX - 12, 292}, {CX + 12, 292}, {CX - 12, 308}, {CX + 12, 308},
                {CX - 24, 292}, {CX + 24, 292}, {CX - 24, 308}, {CX + 24, 308}}) {
            tree(t[0], t[1]);
        }
    }
}
