package com.example.fateapocrypha.world;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Замок Иггдмилления на холме: внешняя стена с башнями и надвратной башней, пандус к воротам, главный корпус
 * с залом (двойная лестница и люстра), галереей и двумя крыльями, дворец с синими шпилями на крыше, внутренний двор.
 * Подвал-лаборатория, покои Хирона и путь побега строятся сюжетными построителями внутри этого замка.
 */
public final class CastleBuilder {
    private static final int Y = CastleSite.Y;
    private static final int X0 = CastleSite.X0;
    private static final int Z0 = CastleSite.Z0;
    private static final int X1 = CastleSite.X0 + CastleSite.W;
    private static final int Z1 = CastleSite.Z0 + CastleSite.D;
    private static final int CX = CastleSite.CX;
    private static final int KX1 = CastleSite.KX1;
    private static final int KX2 = CastleSite.KX2;
    private static final int KZ1 = CastleSite.KZ1;
    private static final int KZ2 = CastleSite.KZ2;

    private static final BlockState AIR = BuildKit.AIR;
    private final BuildKit k;
    private final ServerLevel level;

    private CastleBuilder(ServerLevel level) {
        this.level = level;
        this.k = new BuildKit(level);
    }

    public static void build(ServerLevel level) {
        new CastleBuilder(level).run();
    }

    private void run() {
        curtainWall();
        gatehouse();
        ramp();
        keepShell();
        keepOpenings();
        greatHall();
        gallery();
        wings();
        palace();
        towers();
        courtyard();
        CastleInterior.build(level);   // обстановка: тронный зал, мастерские двора, ресурсы в крыльях
    }

    // ================= материалы =================
    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** Кладка стены: серый кирпич с трещинами, мхом, андезитом и редкими красными заплатами внизу. */
    private BlockState brick(int x, int y, int z) {
        int h = Math.floorMod(hash(x, y, z), 100);
        if (h < 7) {
            return s(Blocks.CRACKED_STONE_BRICKS);
        }
        if (h < 14) {
            return s(Blocks.ANDESITE);
        }
        if (h < 18) {
            return s(Blocks.MOSSY_STONE_BRICKS);
        }
        if (h < 22 && y < Y + 9) {
            return s(Blocks.BRICKS);
        }
        return s(Blocks.STONE_BRICKS);
    }

    /** Светлый камень дворца и зала. */
    private BlockState marble(int x, int y, int z) {
        int h = Math.floorMod(hash(x, y, z), 100);
        if (h < 12) {
            return s(Blocks.POLISHED_DIORITE);
        }
        if (h < 18) {
            return s(Blocks.QUARTZ_BLOCK);
        }
        return s(Blocks.CALCITE);
    }

    private void set(int x, int y, int z, BlockState st) {
        k.set(x, y, z, st);
    }

    private void box(int x1, int y1, int z1, int x2, int y2, int z2, BlockState st) {
        k.fill(x1, y1, z1, x2, y2, z2, st);
    }

    private void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        k.fill(x1, y1, z1, x2, y2, z2, AIR);
    }

    private void brickBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, brick(x, y, z));
                }
            }
        }
    }

    private void marbleBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, marble(x, y, z));
                }
            }
        }
    }

    /** Зубцы по периметру прямоугольника на высоте y (через один). */
    private void merlons(int x1, int z1, int x2, int z2, int y) {
        BlockState st = s(Blocks.STONE_BRICKS);
        for (int x = x1; x <= x2; x++) {
            if (((x - x1) >> 1 & 1) == 0) {
                box(x, y, z1, x, y + 1, z1, st);
                box(x, y, z2, x, y + 1, z2, st);
            }
        }
        for (int z = z1; z <= z2; z++) {
            if (((z - z1) >> 1 & 1) == 0) {
                box(x1, y, z, x1, y + 1, z, st);
                box(x2, y, z, x2, y + 1, z, st);
            }
        }
    }

    /** Кольцо между радиусами (inner, outer] на уровне y. */
    private void ring(int cx, int cz, double outer, double inner, int y, BlockState st, boolean mixed, boolean white) {
        int r = (int) Math.ceil(outer);
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= outer && d > inner) {
                    set(x, y, z, mixed ? (white ? marble(x, y, z) : brick(x, y, z)) : st);
                }
            }
        }
    }

    private void disc(int cx, int cz, double radius, int y, BlockState st) {
        ring(cx, cz, radius, -1.0D, y, st, false, false);
    }

    /** Круглая башня с зубцами и синей конической крышей. */
    private void tower(int cx, int cz, int r, int y0, int h, int coneH, boolean white) {
        // освобождаем внутренность (башня может пересекать стену)
        for (int y = y0 + 1; y <= y0 + h; y++) {
            disc(cx, cz, r - 1.5D, y, AIR);
        }
        for (int y = y0; y <= y0 + h; y++) {
            ring(cx, cz, r + 0.5D, r - 1.5D, y, AIR, true, white);
        }
        for (int y = y0 + 10; y < y0 + h; y += 10) {
            disc(cx, cz, r - 1.5D, y, s(Blocks.SPRUCE_PLANKS));
        }
        disc(cx, cz, r - 1.5D, y0 + h, s(Blocks.STONE_BRICKS));
        // бойницы
        for (int a = 0; a < 360; a += 45) {
            double rad = Math.toRadians(a);
            for (int y : new int[] {y0 + 5, y0 + 15}) {
                if (y + 1 > y0 + h) {
                    continue;
                }
                for (double rr : new double[] {r - 0.5D, r + 0.2D}) {
                    int x = cx + (int) Math.round(rr * Math.cos(rad));
                    int z = cz + (int) Math.round(rr * Math.sin(rad));
                    box(x, y, z, x, y + 1, z, AIR);
                }
            }
        }
        // зубцы
        int idx = 0;
        for (int a = 0; a < 360; a += Math.max(8, (int) (360.0D / (2.0D * Math.PI * r) * 1.0D))) {
            if ((idx++ & 1) == 0) {
                double rad = Math.toRadians(a);
                int x = cx + (int) Math.round((r + 0.0D) * Math.cos(rad));
                int z = cz + (int) Math.round((r + 0.0D) * Math.sin(rad));
                box(x, y0 + h + 1, z, x, y0 + h + 2, z, s(Blocks.STONE_BRICKS));
            }
        }
        // коническая крыша (синий сланец)
        BlockState roof = s(Blocks.BLUE_TERRACOTTA);
        int yy = y0 + h + 2;
        for (int kk = 0; kk <= coneH; kk++) {
            double rr = (r + 1.2D) * (1.0D - (double) kk / coneH);
            if (rr < 0.7D) {
                break;
            }
            ring(cx, cz, rr, Math.max(-1.0D, rr - 2.0D), yy + kk, roof, false, false);
            if (kk % 4 == 3) {
                ring(cx, cz, rr + 0.3D, rr - 0.3D, yy + kk, s(Blocks.LIGHT_BLUE_TERRACOTTA), false, false);
            }
        }
        for (int i = 0; i < 4; i++) {
            set(cx, yy + coneH - 1 + i, cz, s(Blocks.LIGHTNING_ROD));
        }
    }

    // ================= внешняя стена и ворота =================
    private void curtainWall() {
        brickBox(X0, Y, Z0, X1, Y + 13, Z0 + 3);                       // северная
        brickBox(X0, Y, Z0, X0 + 3, Y + 13, Z1);                       // западная
        brickBox(X1 - 3, Y, Z0, X1, Y + 13, Z1);                       // восточная
        brickBox(X0, Y, Z1 - 3, CX - 16, Y + 13, Z1);                  // южная слева от ворот
        brickBox(CX + 16, Y, Z1 - 3, X1, Y + 13, Z1);                  // южная справа от ворот
        // парапеты с двух сторон хода по стене
        merlons(X0, Z0, X1, Z1, Y + 14);
        merlons(X0 + 3, Z0 + 3, X1 - 3, Z1 - 3, Y + 14);
        // пояс камня под ходом
        for (int x = X0; x <= X1; x++) {
            set(x, Y + 9, Z0, s(Blocks.POLISHED_ANDESITE));
        }
        for (int z = Z0; z <= Z1; z++) {
            set(X0, Y + 9, z, s(Blocks.POLISHED_ANDESITE));
            set(X1, Y + 9, z, s(Blocks.POLISHED_ANDESITE));
        }
        // бойницы стены
        for (int x = X0 + 8; x < X1 - 6; x += 7) {
            if (Math.abs(x - CX) > 18) {
                box(x, Y + 6, Z0, x, Y + 8, Z0, AIR);
                box(x, Y + 6, Z1, x, Y + 8, Z1, AIR);
            }
        }
        for (int z = Z0 + 8; z < Z1 - 6; z += 7) {
            box(X0, Y + 6, z, X0, Y + 8, z, AIR);
            box(X1, Y + 6, z, X1, Y + 8, z, AIR);
        }
    }

    private void gatehouse() {
        int zA = Z1 - 8;
        int zB = Z1 + 2;
        brickBox(CX - 15, Y, zA, CX - 4, Y + 27, zB);
        brickBox(CX + 4, Y, zA, CX + 15, Y + 27, zB);
        brickBox(CX - 3, Y + 10, zA, CX + 3, Y + 27, zB);
        clear(CX - 3, Y + 1, zA, CX + 3, Y + 9, zB);
        for (int x = CX - 3; x <= CX + 3; x++) {
            for (int z = zA; z <= zB; z++) {
                set(x, Y, z, s(Blocks.POLISHED_ANDESITE));
            }
        }
        for (int z = zA; z <= zB; z++) {
            set(CX - 3, Y + 9, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
            set(CX + 3, Y + 9, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
        }
        for (int x = CX - 2; x <= CX + 2; x++) {
            for (int y = Y + 7; y <= Y + 9; y++) {
                set(x, y, zB - 1, s(Blocks.IRON_BARS));
            }
        }
        set(CX, Y + 9, zA + 3, BuildKit.HANGING_LANTERN);
        set(CX, Y + 9, zA + 7, BuildKit.HANGING_LANTERN);
        // зубцы и синие пирамидальные крыши башен ворот
        merlons(CX - 15, zA, CX - 4, zB, Y + 28);
        merlons(CX + 4, zA, CX + 15, zB, Y + 28);
        pyramid(CX - 14, zA + 1, CX - 5, zB - 1, Y + 28);
        pyramid(CX + 5, zA + 1, CX + 14, zB - 1, Y + 28);
        // бойницы и знамёна
        for (int y : new int[] {Y + 8, Y + 16, Y + 23}) {
            for (int x : new int[] {CX - 10, CX + 10}) {
                box(x, y, zB, x, y + 1, zB, AIR);
                box(x, y, zA, x, y + 1, zA, AIR);
            }
        }
        for (int x : new int[] {CX - 10, CX + 10}) {
            set(x, Y + 17, zB + 1, s(Blocks.BLUE_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.SOUTH));
        }
    }

    /** Ступенчатая пирамида из синего сланца над прямоугольной башней. */
    private void pyramid(int x1, int z1, int x2, int z2, int y) {
        int layer = 0;
        while (x1 <= x2 && z1 <= z2 && layer < 12) {
            box(x1, y + 1 + layer, z1, x2, y + 1 + layer, z2, s(layer % 4 == 3 ? Blocks.LIGHT_BLUE_TERRACOTTA : Blocks.BLUE_TERRACOTTA));
            x1++;
            z1++;
            x2--;
            z2--;
            layer++;
        }
        set((x1 + x2) / 2 + 0, y + layer, (z1 + z2) / 2, s(Blocks.LIGHTNING_ROD));
    }

    /** Лестница-пандус к воротам вдоль склона холма (на вершину холма, как на городском скриншоте). */
    private void ramp() {
        for (int z = Z1 + 3; z <= CastleSite.RAMP_Z; z++) {
            int g = TrifasTerrain.height(CX, z);
            boolean riser = TrifasTerrain.height(CX, z + 1) < g;
            for (int x = CX - 6; x <= CX + 6; x++) {
                box(x, g - 3, z, x, g - 1, z, s(Blocks.STONE_BRICKS));
                for (int y = g + 1; y <= g + 6; y++) {
                    k.clearVegetation(x, y, z);
                }
                set(x, g, z, riser ? BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false) : s(Blocks.STONE_BRICKS));
            }
            for (int x : new int[] {CX - 7, CX + 7}) {
                box(x, g - 2, z, x, g + 2, z, s(Blocks.STONE_BRICKS));
                set(x, g + 3, z, s(((z >> 1) & 1) == 0 ? Blocks.STONE_BRICK_SLAB : Blocks.POLISHED_ANDESITE));
                if ((z - Z1) % 8 == 3) {
                    set(x, g + 4, z, s(Blocks.LANTERN));
                }
            }
        }
    }

    // ================= главный корпус =================
    private void keepShell() {
        brickBox(KX1, Y, KZ1, KX2, Y + 22, KZ1 + 3);
        brickBox(KX1, Y, KZ2 - 3, KX2, Y + 22, KZ2);
        brickBox(KX1, Y, KZ1, KX1 + 2, Y + 22, KZ2);
        brickBox(KX2 - 2, Y, KZ1, KX2, Y + 22, KZ2);
        // потолочная плита первого этажа и терраса
        for (int x = KX1; x <= KX2; x++) {
            for (int z = KZ1; z <= KZ2; z++) {
                set(x, Y + 23, z, brick(x, Y + 23, z));
            }
        }
        merlons(KX1, KZ1, KX2, KZ2, Y + 24);
        // пояса на фасаде
        for (int x = KX1; x <= KX2; x++) {
            set(x, Y + 12, KZ1, s(Blocks.POLISHED_ANDESITE));
            set(x, Y + 12, KZ2, s(Blocks.POLISHED_ANDESITE));
        }
        for (int z = KZ1; z <= KZ2; z++) {
            set(KX1, Y + 12, z, s(Blocks.POLISHED_ANDESITE));
            set(KX2, Y + 12, z, s(Blocks.POLISHED_ANDESITE));
        }
        // пол и плиты вторых этажей (галерея и крылья), центральный зал открыт на всю высоту
        for (int x = KX1 + 3; x <= KX2 - 3; x++) {
            for (int z = KZ1 + 4; z <= KZ2 - 4; z++) {
                set(x, Y, z, s(Blocks.POLISHED_DEEPSLATE));
            }
        }
        for (int x = KX1 + 3; x <= KX2 - 3; x++) {
            for (int z = 172; z <= 180; z++) {
                set(x, Y + 12, z, s(Blocks.DARK_OAK_PLANKS));
            }
            for (int z = 181; z <= 244; z++) {
                boolean hall = x >= 184 && x <= 246;
                if (!hall) {
                    set(x, Y + 12, z, s(Blocks.DARK_OAK_PLANKS));
                }
            }
        }
    }

    private void keepOpenings() {
        // окна первого этажа (стёкла в толще стены) и аркада второго этажа
        for (int z = 186; z <= 240; z += 6) {
            box(KX1, Y + 3, z, KX1 + 2, Y + 5, z, s(Blocks.GLASS_PANE));
            box(KX2 - 2, Y + 3, z, KX2, Y + 5, z, s(Blocks.GLASS_PANE));
        }
        for (int z = 184; z <= 240; z += 5) {
            box(KX1, Y + 15, z, KX1 + 2, Y + 18, z + 1, s(Blocks.GLASS_PANE));
            box(KX2 - 2, Y + 15, z, KX2, Y + 18, z + 1, s(Blocks.GLASS_PANE));
        }
        for (int x = 166; x <= 266; x += 6) {
            if (Math.abs(x - CX) > 8) {
                box(x, Y + 3, KZ2 - 3, x, Y + 5, KZ2, s(Blocks.GLASS_PANE));
            }
        }
        for (int x = 166; x <= 266; x += 5) {
            if (Math.abs(x - CX) > 8) {
                box(x, Y + 15, KZ2 - 3, x + 1, Y + 18, KZ2, s(Blocks.GLASS_PANE));
            }
            box(x, Y + 15, KZ1, x + 1, Y + 18, KZ1 + 3, s(Blocks.GLASS_PANE));
        }
        // главный вход: высокая арка в южной стене корпуса
        clear(CX - 3, Y + 1, KZ2 - 3, CX + 3, Y + 9, KZ2);
        for (int z = KZ2 - 3; z <= KZ2; z++) {
            set(CX - 3, Y + 9, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
            set(CX + 3, Y + 9, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
        }
        for (int x = CX - 4; x <= CX + 4; x++) {
            set(x, Y + 10, KZ2, s(Blocks.CHISELED_STONE_BRICKS));
        }
        for (int dx : new int[] {-5, 5}) {
            box(CX + dx, Y + 1, KZ2 + 1, CX + dx, Y + 3, KZ2 + 1, s(Blocks.STONE_BRICK_WALL));
            set(CX + dx, Y + 4, KZ2 + 1, s(Blocks.LANTERN));
        }
    }

    // ================= большой зал =================
    private void greatHall() {
        int hx1 = 186;
        int hx2 = 244;
        int hz1 = 181;
        int hz2 = 243;
        // стены зала: западная и восточная (2 толщиной), северная (общая с галереей)
        marbleBox(184, Y, 181, 185, Y + 22, 244);
        marbleBox(245, Y, 181, 246, Y + 22, 244);
        marbleBox(KX1 + 3, Y, 179, KX2 - 3, Y + 22, 180);
        // пол: тёмный мрамор
        for (int x = hx1; x <= hx2; x++) {
            for (int z = hz1; z <= hz2; z++) {
                set(x, Y, z, ((x + z) & 1) == 0 ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.DEEPSLATE_TILES));
            }
        }
        // красная дорожка от входа
        box(CX - 3, Y + 1, 233, CX + 3, Y + 1, hz2, s(Blocks.RED_CARPET));
        for (int x = CX - 4; x <= CX + 4; x += 8) {
            box(x, Y, 233, x, Y, hz2, s(Blocks.LAPIS_BLOCK));
        }

        // пилястры с золотыми основаниями и капителями, синие панели у пола
        for (int z = 186; z <= 238; z += 8) {
            for (int x : new int[] {186, 244}) {
                box(x, Y + 1, z, x, Y + 21, z, s(Blocks.QUARTZ_PILLAR));
                set(x, Y + 1, z, s(Blocks.GOLD_BLOCK));
                set(x, Y + 21, z, s(Blocks.GOLD_BLOCK));
                set(x, Y + 22, z, s(Blocks.CHISELED_QUARTZ_BLOCK));
            }
            for (int dz = 2; dz <= 6; dz++) {
                box(185, Y + 1, z + dz, 185, Y + 3, z + dz, s(Blocks.LAPIS_BLOCK));
                box(245, Y + 1, z + dz, 245, Y + 3, z + dz, s(Blocks.LAPIS_BLOCK));
                // высокие синие окна-панели с подсветкой за стеклом
                box(185, Y + 7, z + dz, 185, Y + 12, z + dz, s(Blocks.BLUE_STAINED_GLASS_PANE));
                box(184, Y + 7, z + dz, 184, Y + 12, z + dz, s(Blocks.SEA_LANTERN));
                box(245, Y + 7, z + dz, 245, Y + 12, z + dz, s(Blocks.BLUE_STAINED_GLASS_PANE));
                box(246, Y + 7, z + dz, 246, Y + 12, z + dz, s(Blocks.SEA_LANTERN));
                box(185, Y + 15, z + dz, 185, Y + 19, z + dz, s(Blocks.BLUE_STAINED_GLASS_PANE));
                box(184, Y + 15, z + dz, 184, Y + 19, z + dz, s(Blocks.SEA_LANTERN));
                box(245, Y + 15, z + dz, 245, Y + 19, z + dz, s(Blocks.BLUE_STAINED_GLASS_PANE));
                box(246, Y + 15, z + dz, 246, Y + 19, z + dz, s(Blocks.SEA_LANTERN));
            }
        }

        // северная стена: синее знамя в золотой рамке над лестницей и высокие окна по сторонам
        box(CX - 7, Y + 9, 180, CX + 7, Y + 21, 180, s(Blocks.GOLD_BLOCK));
        box(CX - 6, Y + 10, 180, CX + 6, Y + 20, 180, s(Blocks.BLUE_CONCRETE));
        for (int dy = 0; dy < 5; dy++) {
            box(CX - 6 + dy, Y + 10 + dy, 180, CX + 6 - dy, Y + 10 + dy, 180, s(Blocks.WHITE_CONCRETE));
        }
        for (int x : new int[] {192, 238}) {
            box(x - 1, Y + 9, 179, x + 1, Y + 18, 180, s(Blocks.BLUE_STAINED_GLASS_PANE));
        }

        // потолочные балки
        for (int z : new int[] {195, 215, 235}) {
            box(hx1, Y + 22, z, hx2, Y + 22, z, s(Blocks.POLISHED_ANDESITE));
        }

        // центральный марш: 8 ступеней белого мрамора
        for (int i = 1; i <= 8; i++) {
            int z = 233 - i;
            for (int x = CX - 7; x <= CX + 7; x++) {
                box(x, Y + 1, z, x, Y + i - 1, z, s(Blocks.QUARTZ_BLOCK));
                set(x, Y + i, z, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
            }
        }
        // площадка
        for (int x = CX - 15; x <= CX + 15; x++) {
            for (int z = 217; z <= 224; z++) {
                box(x, Y + 1, z, x, Y + 7, z, s(Blocks.QUARTZ_BLOCK));
                set(x, Y + 8, z, s(Blocks.SMOOTH_QUARTZ));
            }
        }
        box(CX - 3, Y + 9, 217, CX + 3, Y + 9, 224, s(Blocks.RED_CARPET));
        // боковые марши на балкон
        for (int i = 1; i <= 8; i++) {
            int z = 217 - i;
            for (int x : new int[] {CX - 15, CX + 10}) {
                for (int dx = 0; dx <= 5; dx++) {
                    box(x + dx, Y + 1, z, x + dx, Y + 7 + i, z, s(Blocks.QUARTZ_BLOCK));
                    set(x + dx, Y + 8 + i, z, BuildKit.stairs(Blocks.QUARTZ_STAIRS, Direction.NORTH, false));
                }
            }
        }
        // балкон на уровне второго этажа
        for (int x = CX - 24; x <= CX + 24; x++) {
            for (int z = 190; z <= 208; z++) {
                if (z <= 208 && z >= 201 && x > CX - 10 && x < CX + 10) {
                    continue;   // проём над площадкой
                }
                set(x, Y + 16, z, s(Blocks.SMOOTH_QUARTZ));
            }
        }
        for (int x : new int[] {CX - 24, CX - 8, CX + 8, CX + 24}) {
            box(x, Y + 1, 190, x, Y + 15, 190, s(Blocks.QUARTZ_PILLAR));
        }
        box(CX - 3, Y + 17, 190, CX + 3, Y + 17, 200, s(Blocks.RED_CARPET));
        // перила: железные прутья с золотыми столбиками
        for (int x = CX - 24; x <= CX + 24; x++) {
            if (x > CX - 10 && x < CX + 10) {
                continue;
            }
            set(x, Y + 17, 208, s(Blocks.IRON_BARS));
            if ((x - CX) % 4 == 0) {
                set(x, Y + 17, 208, s(Blocks.GOLD_BLOCK));
                set(x, Y + 18, 208, s(Blocks.LANTERN));
            }
        }
        for (int z = 209; z <= 216; z++) {
            for (int x : new int[] {CX - 16, CX - 9, CX + 9, CX + 16}) {
                set(x, Y + 9 + (z - 209) + 1, z, s(Blocks.IRON_BARS));
            }
        }

        // люстра: цепи, золотое кольцо, фонари и стержни
        int lx = CX;
        int lz = 212;
        for (int y = Y + 17; y <= Y + 22; y++) {
            set(lx, y, lz, s(Blocks.CHAIN));
        }
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            int ox = lx + (int) Math.round(5.0D * Math.cos(rad));
            int oz = lz + (int) Math.round(5.0D * Math.sin(rad));
            set(ox, Y + 17, oz, s(Blocks.GOLD_BLOCK));
            set(ox, Y + 16, oz, BuildKit.HANGING_LANTERN);
            int ix = lx + (int) Math.round(3.0D * Math.cos(rad));
            int iz = lz + (int) Math.round(3.0D * Math.sin(rad));
            set(ix, Y + 15, iz, s(Blocks.END_ROD).setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.DOWN));
        }
        for (int a = 0; a < 360; a += 15) {
            double rad = Math.toRadians(a);
            int ox = lx + (int) Math.round(5.0D * Math.cos(rad));
            int oz = lz + (int) Math.round(5.0D * Math.sin(rad));
            set(ox, Y + 18, oz, s(Blocks.CHAIN));
        }
        for (int dx = -5; dx <= 5; dx++) {
            set(lx + dx, Y + 19, lz, s(Blocks.CHAIN));
            set(lx, Y + 19, lz + dx, s(Blocks.CHAIN));
        }
        set(lx, Y + 16, lz, s(Blocks.GLOWSTONE));
        set(lx, Y + 15, lz, s(Blocks.SEA_LANTERN));

        // двустворчатые проёмы в крылья и в галерею
        for (int z : new int[] {200, 226}) {
            clear(184, Y + 1, z, 185, Y + 7, z + 4);
            clear(245, Y + 1, z, 246, Y + 7, z + 4);
        }
        for (int x : new int[] {195, 231}) {
            clear(x, Y + 1, 179, x + 4, Y + 8, 180);
        }
    }

    // ================= галерея (бывший коридор падения) =================
    private void gallery() {
        int gx1 = KX1 + 3;
        int gx2 = KX2 - 3;
        // пол: тёмный полированный камень, синяя кайма и золотые линии
        for (int x = gx1; x <= gx2; x++) {
            for (int z = 172; z <= 178; z++) {
                set(x, Y, z, s(Blocks.POLISHED_DEEPSLATE));
            }
            set(x, Y, 172, s(Blocks.LAPIS_BLOCK));
            set(x, Y, 178, s(Blocks.LAPIS_BLOCK));
            set(x, Y, 173, s(Blocks.GOLD_BLOCK));
            set(x, Y, 177, s(Blocks.GOLD_BLOCK));
        }
        // внутренняя облицовка северной стены толщиной 4: светлый камень
        box(gx1, Y + 1, 171, gx2, Y + 11, 171, s(Blocks.CALCITE));
        // высокие стрельчатые окна на север
        for (int cx = 171; cx <= 263; cx += 8) {
            for (int dx = -1; dx <= 1; dx++) {
                clear(cx + dx, Y + 2, 169, cx + dx, Y + 8, 171);
                box(cx + dx, Y + 2, 168, cx + dx, Y + 8, 168, s(Blocks.GLASS_PANE));
            }
            clear(cx, Y + 9, 169, cx, Y + 9, 171);
            set(cx, Y + 9, 168, s(Blocks.GLASS_PANE));
            // пилястры между окнами, золотые капители
            int px = cx + 4;
            if (px <= gx2) {
                box(px, Y + 1, 172, px, Y + 10, 172, s(Blocks.QUARTZ_PILLAR));
                set(px, Y + 1, 172, s(Blocks.GOLD_BLOCK));
                set(px, Y + 10, 172, s(Blocks.GOLD_BLOCK));
                box(px, Y + 1, 178, px, Y + 10, 178, s(Blocks.QUARTZ_PILLAR));
                set(px, Y + 1, 178, s(Blocks.GOLD_BLOCK));
                set(px, Y + 10, 178, s(Blocks.GOLD_BLOCK));
                // картина на южной стене: золотая рама и синее полотно
                if (px + 5 <= gx2) {
                    box(px + 1, Y + 4, 179, px + 5, Y + 8, 179, s(Blocks.GOLD_BLOCK));
                    box(px + 2, Y + 5, 179, px + 4, Y + 7, 179, s(((px >> 3) & 1) == 0 ? Blocks.BLUE_TERRACOTTA : Blocks.LIGHT_BLUE_TERRACOTTA));
                }
            }
        }
        // свод: балки и бра, люстры
        for (int x = gx1; x <= gx2; x += 8) {
            box(x, Y + 11, 172, x, Y + 11, 178, s(Blocks.POLISHED_ANDESITE));
            set(x, Y + 10, 172, s(Blocks.STONE_BRICK_STAIRS).setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH)
                    .setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
            set(x, Y + 10, 178, s(Blocks.STONE_BRICK_STAIRS).setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH)
                    .setValue(net.minecraft.world.level.block.StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP));
        }
        for (int x = gx1 + 12; x <= gx2 - 8; x += 16) {
            for (int y = Y + 11; y >= Y + 9; y--) {
                set(x, y, 175, s(Blocks.CHAIN));
            }
            set(x, Y + 8, 175, BuildKit.HANGING_LANTERN);
            for (int dx : new int[] {-2, 2}) {
                set(x + dx, Y + 9, 175, s(Blocks.GOLD_BLOCK));
                set(x + dx, Y + 8, 175, BuildKit.HANGING_LANTERN);
            }
            set(x - 1, Y + 10, 175, s(Blocks.GOLD_BLOCK));
            set(x + 1, Y + 10, 175, s(Blocks.GOLD_BLOCK));
        }
        // дверь галереи в крылья
        for (int x : new int[] {172, 254}) {
            clear(x, Y + 1, 179, x + 4, Y + 8, 180);
        }
        // южные бра
        for (int x = gx1 + 6; x <= gx2; x += 8) {
            set(x, Y + 5, 178, s(Blocks.LANTERN));
        }
    }

    // ================= крылья корпуса =================
    private void wings() {
        for (int side = 0; side < 2; side++) {
            int x1 = side == 0 ? KX1 + 3 : 247;
            int x2 = side == 0 ? 183 : KX2 - 3;
            // перегородка у галереи уже есть (z=179..180), пол первого этажа
            for (int x = x1; x <= x2; x++) {
                for (int z = 181; z <= 244; z++) {
                    set(x, Y, z, s(Blocks.DARK_OAK_PLANKS));
                    set(x, Y + 12, z, s(Blocks.DARK_OAK_PLANKS));
                }
            }
            // первый этаж: ковёр, люстры
            box(x1 + 5, Y + 1, 183, x2 - 5, Y + 1, 242, s(side == 0 ? Blocks.BLUE_CARPET : Blocks.RED_CARPET));
            for (int z = 190; z <= 238; z += 12) {
                int cx = (x1 + x2) / 2;
                for (int y = Y + 11; y >= Y + 9; y--) {
                    set(cx, y, z, s(Blocks.CHAIN));
                }
                set(cx, Y + 8, z, BuildKit.HANGING_LANTERN);
            }
            if (side == 0) {
                // библиотека
                for (int z = 184; z <= 240; z += 2) {
                    box(x1, Y + 1, z, x1, Y + 4, z, s(Blocks.BOOKSHELF));
                    box(x1, Y + 1, z + 1, x1, Y + 4, z + 1, s(Blocks.BOOKSHELF));
                }
                for (int z = 190; z <= 236; z += 8) {
                    box(x1 + 3, Y + 1, z, x1 + 8, Y + 1, z, s(Blocks.SPRUCE_PLANKS));
                    set(x1 + 3, Y + 2, z, s(Blocks.LECTERN));
                }
            } else {
                // пиршественный зал: длинные столы
                for (int z = 186; z <= 240; z += 6) {
                    box(x1 + 6, Y + 1, z, x2 - 6, Y + 1, z, s(Blocks.SPRUCE_PLANKS));
                    box(x1 + 6, Y + 2, z, x2 - 6, Y + 2, z, s(Blocks.SPRUCE_PRESSURE_PLATE));
                    box(x1 + 6, Y + 1, z - 1, x2 - 6, Y + 1, z - 1, s(Blocks.SPRUCE_SLAB));
                    box(x1 + 6, Y + 1, z + 1, x2 - 6, Y + 1, z + 1, s(Blocks.SPRUCE_SLAB));
                }
            }
            // второй этаж: комнаты с кроватями и фонарями
            for (int z = 184; z <= 240; z += 8) {
                int bx = side == 0 ? x1 + 1 : x2 - 2;
                set(bx, Y + 13, z, s(Blocks.RED_BED).setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                        .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
                set(bx, Y + 13, z - 1, s(Blocks.RED_BED).setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                        .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
                set(side == 0 ? x1 + 4 : x2 - 4, Y + 13, z, s(Blocks.BARREL));
                set((x1 + x2) / 2, Y + 22, z, BuildKit.HANGING_LANTERN);
            }
            box(x1 + 4, Y + 13, 183, x2 - 4, Y + 13, 242, s(Blocks.GRAY_CARPET));
            // лестница на второй этаж вдоль южной части крыла
            int sx = side == 0 ? x1 : x2 - 2;
            for (int i = 1; i <= 12; i++) {
                for (int dx = 0; dx <= 2; dx++) {
                    box(sx + dx, Y + 1, 242 - i, sx + dx, Y + i - 1, 242 - i, s(Blocks.STONE_BRICKS));
                    set(sx + dx, Y + i, 242 - i, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
                }
            }
            for (int dx = 0; dx <= 2; dx++) {
                clear(sx + dx, Y + 12, 229, sx + dx, Y + 12, 241);
            }
        }
    }

    // ================= дворец на крыше =================
    private void palace() {
        int px1 = 190;
        int px2 = 240;
        int pz1 = 182;
        int pz2 = 208;
        int base = Y + 24;
        // стены и пол
        marbleBox(px1, base, pz1, px2, base + 11, pz1 + 1);
        marbleBox(px1, base, pz2 - 1, px2, base + 11, pz2);
        marbleBox(px1, base, pz1, px1 + 1, base + 11, pz2);
        marbleBox(px2 - 1, base, pz1, px2, base + 11, pz2);
        // пояса и окна
        for (int x = px1; x <= px2; x++) {
            set(x, base + 5, pz1, s(Blocks.POLISHED_DIORITE));
            set(x, base + 5, pz2, s(Blocks.POLISHED_DIORITE));
        }
        for (int x = px1 + 3; x < px2 - 2; x += 4) {
            box(x, base + 2, pz1, x, base + 4, pz1 + 1, s(Blocks.GLASS_PANE));
            box(x, base + 2, pz2 - 1, x, base + 4, pz2, s(Blocks.GLASS_PANE));
            box(x, base + 7, pz1, x, base + 9, pz1 + 1, s(Blocks.GLASS_PANE));
            box(x, base + 7, pz2 - 1, x, base + 9, pz2, s(Blocks.GLASS_PANE));
        }
        for (int z = pz1 + 3; z < pz2 - 2; z += 4) {
            box(px1, base + 2, z, px1 + 1, base + 4, z, s(Blocks.GLASS_PANE));
            box(px2 - 1, base + 2, z, px2, base + 4, z, s(Blocks.GLASS_PANE));
        }
        // дверь с террасы
        clear(CX - 2, base, pz2 - 1, CX + 2, base + 5, pz2);
        // двускатная крыша из тёмного сланца по оси x
        int rows = (pz2 - pz1) / 2;
        for (int kk = 0; kk <= rows; kk++) {
            int y = base + 12 + kk;
            for (int x = px1 - 1; x <= px2 + 1; x++) {
                if (kk < rows) {
                    set(x, y, pz1 + kk, BuildKit.stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, false));
                    set(x, y, pz2 - kk, BuildKit.stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, false));
                } else {
                    set(x, y, pz1 + kk, s(Blocks.DEEPSLATE_TILE_SLAB));
                }
            }
            // фронтоны
            for (int z = pz1 + kk + 1; z <= pz2 - kk - 1; z++) {
                set(px1, y, z, marble(px1, y, z));
                set(px2, y, z, marble(px2, y, z));
            }
        }
        for (int x : new int[] {px1 - 1, px2 + 1}) {
            for (int i = 0; i < 3; i++) {
                set(x, base + 12 + rows + 1 + i, (pz1 + pz2) / 2, s(Blocks.LIGHTNING_ROD));
            }
        }
        // слуховые окна на южном скате
        for (int x = px1 + 8; x <= px2 - 8; x += 10) {
            clear(x - 1, base + 12, pz2 - 3, x + 1, base + 13, pz2 - 1);
            box(x - 1, base + 12, pz2 - 3, x + 1, base + 12, pz2 - 3, s(Blocks.GLASS_PANE));
            for (int dx = -2; dx <= 2; dx++) {
                set(x + dx, base + 14, pz2 - 3, BuildKit.stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, false));
            }
        }
    }

    // ================= башни =================
    private void towers() {
        // угловые башни внешней стены и промежуточные
        tower(X0 + 3, Z0 + 3, 8, Y, 26, 11, false);
        tower(X1 - 3, Z0 + 3, 8, Y, 26, 11, false);
        tower(X0 + 3, Z1 - 3, 8, Y, 26, 11, false);
        tower(X1 - 3, Z1 - 3, 8, Y, 26, 11, false);
        tower(CX, Z0 + 1, 6, Y, 21, 9, false);
        tower(X0 + 1, CastleSite.CZ, 6, Y, 21, 9, false);
        tower(X1 - 1, CastleSite.CZ, 6, Y, 21, 9, false);
        // угловые башни корпуса
        tower(KX1 + 1, KZ1 + 1, 8, Y, 31, 13, false);
        tower(KX2 - 1, KZ1 + 1, 8, Y, 31, 13, false);
        tower(KX1 + 1, KZ2 - 1, 8, Y, 31, 13, false);
        tower(KX2 - 1, KZ2 - 1, 8, Y, 31, 13, false);
        // белые башни дворца и высокая центральная башня
        tower(191, 183, 4, Y + 23, 22, 10, true);
        tower(239, 183, 4, Y + 23, 22, 10, true);
        tower(191, 207, 4, Y + 23, 22, 10, true);
        tower(239, 207, 4, Y + 23, 22, 10, true);
        tower(CX, 176, 6, Y + 23, 32, 16, true);
    }

    // ================= двор =================
    private void courtyard() {
        for (int x = X0 + 4; x <= X1 - 4; x++) {
            for (int z = Z0 + 4; z <= Z1 - 4; z++) {
                boolean insideKeep = x >= KX1 && x <= KX2 && z >= KZ1 && z <= KZ2;
                if (insideKeep) {
                    continue;
                }
                int h = Math.floorMod(hash(x, 0, z), 100);
                BlockState st;
                if (Math.abs(x - CX) <= 4 && z > KZ2) {
                    st = Math.abs(x - CX) == 4 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.STONE_BRICKS);
                } else if (h < 28) {
                    st = s(Blocks.GRAVEL);
                } else if (h < 52) {
                    st = s(Blocks.COBBLESTONE);
                } else if (h < 66) {
                    st = s(Blocks.COARSE_DIRT);
                } else if (h < 72) {
                    st = s(Blocks.STONE);
                } else {
                    continue;
                }
                set(x, Y, z, st);
                for (int y = Y + 1; y <= Y + 4; y++) {
                    k.clearVegetation(x, y, z);
                }
            }
        }
        // фонтан
        int fx = CX;
        int fz = 272;
        for (int x = fx - 7; x <= fx + 7; x++) {
            for (int z = fz - 7; z <= fz + 7; z++) {
                double d = Math.hypot(x - fx, z - fz);
                if (d <= 6.5D && d > 5.2D) {
                    set(x, Y, z, s(Blocks.STONE_BRICKS));
                    set(x, Y + 1, z, s(Blocks.STONE_BRICK_WALL));
                } else if (d <= 5.2D) {
                    set(x, Y - 1, z, s(Blocks.STONE_BRICKS));
                    set(x, Y, z, s(Blocks.WATER));
                }
            }
        }
        box(fx, Y, fz, fx, Y + 4, fz, s(Blocks.QUARTZ_PILLAR));
        set(fx, Y + 5, fz, s(Blocks.SEA_LANTERN));
        // фонари вдоль главной дороги
        for (int z = 262; z <= 342; z += 12) {
            for (int x : new int[] {CX - 6, CX + 6}) {
                box(x, Y + 1, z, x, Y + 3, z, s(Blocks.STONE_BRICK_WALL));
                set(x, Y + 4, z, s(Blocks.LANTERN));
            }
        }
        // лестницы на стену у западной и восточной сторон двора
        for (int i = 1; i <= 13; i++) {
            for (int dx = 0; dx <= 2; dx++) {
                for (int side = 0; side < 2; side++) {
                    int x = side == 0 ? X0 + 4 + dx : X1 - 6 + dx;
                    int z = 342 - i;
                    box(x, Y, z, x, Y + i - 1, z, s(Blocks.STONE_BRICKS));
                    set(x, Y + i, z, BuildKit.stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
                }
            }
        }
        // хозяйственные постройки: кузница, склады
        hut(X0 + 8, 255, X0 + 20, 266);
        hut(X1 - 20, 255, X1 - 8, 266);
        hut(X0 + 8, 326, X0 + 20, 337);
        hut(X1 - 20, 326, X1 - 8, 337);
    }

    /** Каменная хозяйственная постройка с плоской крышей. */
    private void hut(int x1, int z1, int x2, int z2) {
        brickBox(x1, Y + 1, z1, x2, Y + 4, z2);
        clear(x1 + 1, Y + 1, z1 + 1, x2 - 1, Y + 4, z2 - 1);
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                set(x, Y + 5, z, s(Blocks.DEEPSLATE_TILE_SLAB));
            }
        }
        int mid = (x1 + x2) / 2;
        k.door(Blocks.SPRUCE_DOOR, mid, Y + 1, z2, Direction.SOUTH);
        for (int x = x1 + 2; x < x2 - 1; x += 3) {
            box(x, Y + 2, z1, x, Y + 3, z1, s(Blocks.GLASS_PANE));
        }
        set(x1 + 1, Y + 1, z1 + 1, s(Blocks.FURNACE));
        set(x1 + 2, Y + 1, z1 + 1, s(Blocks.ANVIL));
        set(x2 - 1, Y + 1, z1 + 1, s(Blocks.BARREL));
        set(mid, Y + 4, (z1 + z2) / 2, BuildKit.HANGING_LANTERN);
    }
}
