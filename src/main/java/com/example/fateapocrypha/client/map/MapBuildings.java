package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.scene.Reloc;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.world.CastleSite;
import com.example.fateapocrypha.world.TrifasRoads;
import com.example.fateapocrypha.world.TrifasTerrain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Постройки на карте мира: вместо плоской заливки замок, ферма, улицы, часовня, сады и случайные постройки мира
 * (дома, башни, колодцы, лагеря, святилища, руины) рисуются как маленькие объёмные здания: стены, крыши, башни с
 * коническими крышами, тени. Проекция вертикальная косая: точка (x, z, высота) попадает на экран в
 * (x, z - высота * K). Размеры и места взяты из тех же координат, по которым постройки ставит мир (CastleBuilder,
 * StoryWorld, WorldDecor), поэтому карта совпадает с тем, что игрок увидит в мире.
 */
final class MapBuildings {
    enum Roof { NONE, GABLE_X, GABLE_Z, HIP, STEPPED, CONE, DOME }

    /**
     * Здание. Прямоугольное: x1..x2, z1..z2 (включительно). Круглое: (x1, z1) центр, x2 радиус.
     * base: на сколько блоков здание приподнято над землёй (башни на крыше корпуса), h: высота стен, roofH: высота крыши.
     */
    record Block(StoryLocation loc, boolean round, double x1, double z1, double x2, double z2,
                 double base, double h, double roofH, Roof roof, int wall, int top, float minZoom) {
    }

    /** Плоское пятно на земле: двор, поле, мостовая, водоём. Круглое: (x1, z1) центр, x2 радиус. */
    record Patch(StoryLocation loc, boolean disc, double x1, double z1, double x2, double z2,
                 int color, int stripe, float minZoom) {
    }

    /** Сколько пикселей экрана даёт один блок высоты на единицу масштаба карты. */
    private static final float K = 0.62F;

    private static final List<Block> BLOCKS = new ArrayList<>();
    private static final List<Patch> PATCHES = new ArrayList<>();
    private static volatile List<Block> decorBlocks = List.of();
    private static volatile List<Patch> decorPatches = List.of();
    private static volatile boolean decorStarted = false;

    private static final List<Block> VISIBLE = new ArrayList<>();
    private static final Comparator<Block> DEPTH = Comparator.comparingDouble(MapBuildings::depth);

    // цвета
    private static final int STONE = 0xC9C3BA;
    private static final int STONE_TOP = 0xDDD8CF;
    private static final int STONE_DARK = 0xA7A198;
    private static final int BLUE_ROOF = 0x5B86C9;
    private static final int BLUE_ROOF_LIGHT = 0x7CA6E4;
    private static final int WHITE = 0xF3F1EC;
    private static final int COPPER = 0x6FB5A6;
    private static final int WOOD = 0xB38B54;
    private static final int WOOD_DARK = 0x5A4126;
    private static final int PAVING = 0xD9D3C8;
    private static final int[] ROOFS = {0x5E6A7D, 0x5FA89A, 0xD98BA3, 0xB5533C};

    private MapBuildings() {
    }

    // ===================== список построек =====================
    private static void add(StoryLocation loc, double x1, double z1, double x2, double z2, double base, double h,
                            double roofH, Roof roof, int wall, int top, float minZoom) {
        BLOCKS.add(new Block(loc, false, x1, z1, x2, z2, base, h, roofH, roof, wall, top, minZoom));
    }

    private static void tower(StoryLocation loc, double cx, double cz, double r, double base, double h,
                              double roofH, Roof roof, int wall, int top, float minZoom) {
        BLOCKS.add(new Block(loc, true, cx, cz, r, 0.0D, base, h, roofH, roof, wall, top, minZoom));
    }

    private static void rect(StoryLocation loc, double x1, double z1, double x2, double z2, int color, int stripe, float minZoom) {
        PATCHES.add(new Patch(loc, false, x1, z1, x2, z2, color, stripe, minZoom));
    }

    private static void disc(StoryLocation loc, double cx, double cz, double r, int color, float minZoom) {
        PATCHES.add(new Patch(loc, true, cx, cz, r, 0.0D, color, 0, minZoom));
    }

    /** Здание в сюжетных координатах: переводится в мировые слоем Reloc. */
    private static void sadd(StoryLocation loc, int x1, int z1, int x2, int z2, double h, double roofH, Roof roof,
                             int wall, int top, float minZoom) {
        int[] r = Reloc.rect(x1, z1, x2, z2);
        add(loc, r[0], r[1], r[2], r[3], 0.0D, h, roofH, roof, wall, top, minZoom);
    }

    private static void srect(StoryLocation loc, int x1, int z1, int x2, int z2, int color, int stripe, float minZoom) {
        int[] r = Reloc.rect(x1, z1, x2, z2);
        rect(loc, r[0], r[1], r[2] + 1, r[3] + 1, color, stripe, minZoom);
    }

    private static void sdisc(StoryLocation loc, double cx, double cz, double r, int color, float minZoom) {
        disc(loc, Reloc.x(cx, cz) + 0.5D, Reloc.z(cx, cz) + 0.5D, r, color, minZoom);
    }

    static {
        try {
            buildCastle();
            buildFarm();
            buildRedCamp();
            buildChapel();
            buildRuins();
            buildTowns();
            buildPlain();
            buildGardens();
            buildGates();
        } catch (RuntimeException e) {
            // карта работает и без построек
        }
    }

    /** Замок Иггдмилления (CastleBuilder): стена с зубцами, семь башен, корпус с угловыми башнями, дворец, ворота. */
    private static void buildCastle() {
        StoryLocation c = StoryLocation.CASTLE_LAB;
        int x0 = CastleSite.X0;
        int z0 = CastleSite.Z0;
        int x1 = CastleSite.X0 + CastleSite.W;
        int z1 = CastleSite.Z0 + CastleSite.D;
        int cx = CastleSite.CX;
        int cz = CastleSite.CZ;

        // земля: газон двора, мощение вокруг корпуса, дорога, площадь, фонтан, пандус
        rect(c, x0 + 4, z0 + 4, x1 - 3, z1 - 3, 0xBFDDAE, 0, 0.18F);
        rect(c, CastleSite.KX1 - 4, CastleSite.KZ1 - 4, CastleSite.KX2 + 5, CastleSite.KZ2 + 5, PAVING, 0, 0.18F);
        rect(c, cx - 4, CastleSite.KZ2 + 1, cx + 5, z1 - 2, PAVING, 0, 0.18F);
        rect(c, x0 + 6, 297, x1 - 5, 300, PAVING, 0, 0.3F);
        disc(c, cx + 0.5D, 272.5D, 13.0D, PAVING, 0.18F);
        disc(c, cx + 0.5D, 272.5D, 6.5D, 0xB9B3AA, 0.18F);
        disc(c, cx + 0.5D, 272.5D, 5.2D, 0x8FCBEF, 0.18F);
        rect(c, cx - 6, z1 + 3, cx + 7, CastleSite.RAMP_Z + 1, STONE_TOP, 0, 0.18F);
        rect(c, 139, 274, 159, 293, 0x9CD08A, 0, 0.4F);   // сад

        // стена (толщина 3, высота 14)
        add(c, x0, z0, x1, z0 + 3, 0, 14, 0, Roof.NONE, STONE, STONE_TOP, 0.18F);
        add(c, x0, z0, x0 + 3, z1, 0, 14, 0, Roof.NONE, STONE, STONE_TOP, 0.18F);
        add(c, x1 - 3, z0, x1, z1, 0, 14, 0, Roof.NONE, STONE, STONE_TOP, 0.18F);
        add(c, x0, z1 - 3, cx - 16, z1, 0, 14, 0, Roof.NONE, STONE, STONE_TOP, 0.18F);
        add(c, cx + 16, z1 - 3, x1, z1, 0, 14, 0, Roof.NONE, STONE, STONE_TOP, 0.18F);

        // угловые и средние башни стены
        tower(c, x0 + 3, z0 + 3, 8, 0, 26, 11, Roof.CONE, STONE, BLUE_ROOF, 0.18F);
        tower(c, x1 - 3, z0 + 3, 8, 0, 26, 11, Roof.CONE, STONE, BLUE_ROOF, 0.18F);
        tower(c, x0 + 3, z1 - 3, 8, 0, 26, 11, Roof.CONE, STONE, BLUE_ROOF, 0.18F);
        tower(c, x1 - 3, z1 - 3, 8, 0, 26, 11, Roof.CONE, STONE, BLUE_ROOF, 0.18F);
        tower(c, cx, z0 + 1, 6, 0, 21, 9, Roof.CONE, STONE, BLUE_ROOF, 0.18F);
        tower(c, x0 + 1, cz, 6, 0, 21, 9, Roof.CONE, STONE, BLUE_ROOF, 0.18F);
        tower(c, x1 - 1, cz, 6, 0, 21, 9, Roof.CONE, STONE, BLUE_ROOF, 0.18F);

        // хозяйственные постройки двора и конюшня
        add(c, 138, 255, 150, 266, 0, 4, 0, Roof.NONE, STONE, 0x6B6F7A, 0.4F);
        add(c, 280, 255, 292, 266, 0, 4, 0, Roof.NONE, STONE, 0x6B6F7A, 0.4F);
        add(c, 138, 326, 150, 337, 0, 4, 0, Roof.NONE, STONE, 0x6B6F7A, 0.4F);
        add(c, 280, 326, 292, 337, 0, 4, 0, Roof.NONE, STONE, 0x6B6F7A, 0.4F);
        add(c, 272, 272, 292, 294, 0, 4, 3, Roof.GABLE_X, 0xB59A74, WOOD_DARK, 0.4F);

        // главный корпус
        add(c, CastleSite.KX1, CastleSite.KZ1, CastleSite.KX2, CastleSite.KZ2, 0, 23, 0, Roof.NONE, 0xCFC9C0, 0xE3DED5, 0.18F);
        tower(c, CastleSite.KX1 + 1, CastleSite.KZ1 + 1, 8, 0, 31, 13, Roof.CONE, 0xCFC9C0, BLUE_ROOF, 0.18F);
        tower(c, CastleSite.KX2 - 1, CastleSite.KZ1 + 1, 8, 0, 31, 13, Roof.CONE, 0xCFC9C0, BLUE_ROOF, 0.18F);
        tower(c, CastleSite.KX1 + 1, CastleSite.KZ2 - 1, 8, 0, 31, 13, Roof.CONE, 0xCFC9C0, BLUE_ROOF, 0.18F);
        tower(c, CastleSite.KX2 - 1, CastleSite.KZ2 - 1, 8, 0, 31, 13, Roof.CONE, 0xCFC9C0, BLUE_ROOF, 0.18F);

        // привратная башня: две башни и арка между ними, пирамидальные синие крыши
        int ga = z1 - 8;
        int gb = z1 + 2;
        add(c, cx - 15, ga, cx - 4, gb, 0, 27, 7, Roof.HIP, STONE, COPPER, 0.18F);
        add(c, cx + 4, ga, cx + 15, gb, 0, 27, 7, Roof.HIP, STONE, COPPER, 0.18F);
        add(c, cx - 3, ga, cx + 3, gb, 10, 17, 0, Roof.NONE, STONE, STONE_TOP, 0.18F);

        // дворец на террасе корпуса: белые башни, мраморные стены, крыша из окисленной меди
        add(c, 190, 182, 240, 208, 24, 11, 8, Roof.GABLE_X, WHITE, COPPER, 0.18F);
        tower(c, 191, 183, 4, 23, 22, 10, Roof.CONE, WHITE, BLUE_ROOF_LIGHT, 0.18F);
        tower(c, 239, 183, 4, 23, 22, 10, Roof.CONE, WHITE, BLUE_ROOF_LIGHT, 0.18F);
        tower(c, 191, 207, 4, 23, 22, 10, Roof.CONE, WHITE, BLUE_ROOF_LIGHT, 0.18F);
        tower(c, 239, 207, 4, 23, 22, 10, Roof.CONE, WHITE, BLUE_ROOF_LIGHT, 0.18F);
        tower(c, cx, 176, 6, 23, 32, 16, Roof.CONE, WHITE, BLUE_ROOF_LIGHT, 0.18F);
    }

    /** Ферма (сюжетные координаты): дом, амбар, поля. */
    private static void buildFarm() {
        StoryLocation f = StoryLocation.FARM;
        srect(f, 1188, 1189, 1190, 1216, 0xD3BC8C, 0, 0.3F);
        srect(f, 1184, 1189, 1194, 1190, 0xD3BC8C, 0, 0.3F);
        srect(f, 1172, 1195, 1186, 1212, 0xE9D071, 0xD2B552, 0.3F);     // пшеница
        srect(f, 1192, 1195, 1210, 1212, 0xA9CF73, 0xE79A52, 0.3F);     // морковь
        sadd(f, 1184, 1180, 1194, 1188, 4, 4, Roof.HIP, WOOD, WOOD_DARK, 0.3F);
        sadd(f, 1196, 1178, 1208, 1190, 6, 5, Roof.HIP, 0xB0513F, WOOD_DARK, 0.3F);
        sadd(f, 1198, 1192, 1200, 1194, 2, 0, Roof.NONE, 0xB59A74, 0xC9B28A, 0.8F);
    }

    /** Лагерь Красных: площадка, палатки из шерсти, костёр. */
    private static void buildRedCamp() {
        StoryLocation r = StoryLocation.RED_CAMP;
        sdisc(r, 800, 800, 16.0D, 0xB5A584, 0.3F);
        sadd(r, 800 - 16, 800 - 10, 800 - 11, 800 - 6, 1, 3, Roof.HIP, 0xC8453F, 0xC8453F, 0.3F);
        sadd(r, 800 - 16, 800 + 6, 800 - 11, 800 + 10, 1, 3, Roof.HIP, 0x8E8E96, 0x8E8E96, 0.3F);
        sadd(r, 800 + 11, 800 - 12, 800 + 16, 800 - 8, 1, 3, Roof.HIP, 0xC8453F, 0xC8453F, 0.3F);
        int[] fire = Reloc.rect(800, 800, 800, 800);
        tower(r, fire[0] + 0.5D, fire[1] + 0.5D, 1.0D, 0, 1, 0, Roof.NONE, 0xE8873A, 0xF7C45A, 0.7F);
    }

    /** Часовня: каменный зал с высоким двускатным сланцевым покрытием. */
    private static void buildChapel() {
        StoryLocation c = StoryLocation.CHAPEL;
        sadd(c, 790, 890, 810, 920, 10, 7, Roof.GABLE_Z, 0xD3CDC3, 0x6D6A8F, 0.3F);
    }

    /** Руины: россыпь булыжника и обломки стен (тот же ряд случайных чисел, что у постройки в StoryWorld). */
    private static void buildRuins() {
        StoryLocation u = StoryLocation.RUINS;
        int cx = 800;
        int cz = 1000;
        srect(u, cx - 20, cz - 20, cx + 20, cz + 20, 0xCBC6BC, 0, 0.3F);
        Random rnd = new Random(99L);
        for (int x = cx - 20; x <= cx + 20; x++) {
            for (int z = cz - 20; z <= cz + 20; z++) {
                rnd.nextInt(10);
            }
        }
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
                    rnd.nextInt(3);
                }
            }
            sadd(u, x, z, alongX ? x + len - 1 : x, alongX ? z : z + len - 1, height, 0, Roof.NONE, 0xA8B09A, 0xBFC6B2, 0.55F);
        }
        for (int[] p : new int[][] {{-14, -9}, {14, -9}, {-14, 9}, {14, 9}}) {
            sadd(u, cx + p[0], cz + p[1], cx + p[0], cz + p[1], 3, 0, Roof.NONE, 0xB1ABA2, 0xCFCAC0, 0.8F);
        }
    }

    /** Улица Трифаса (фахверк) и викторианский Лондон (кирпич): мостовая и два ряда домов. */
    private static void buildTowns() {
        for (int t = 0; t < 2; t++) {
            StoryLocation loc = t == 0 ? StoryLocation.STREET : StoryLocation.LONDON;
            int zc = t == 0 ? 800 : 1000;
            int height = t == 0 ? 7 : 9;
            int wall = t == 0 ? 0xEDE8DC : 0xB5503C;
            srect(loc, 1180, zc - 3, 1220, zc + 3, 0xBDB8AE, 0, 0.3F);
            srect(loc, 1180, zc - 4, 1220, zc - 4, 0xD3CEC4, 0, 0.5F);
            srect(loc, 1180, zc + 4, 1220, zc + 4, 0xD3CEC4, 0, 0.5F);
            int n = 0;
            for (int x = 1180; x < 1220; x += 10) {
                sadd(loc, x, zc + 6, x + 8, zc + 14, height, 5, Roof.GABLE_X, wall, ROOFS[n % ROOFS.length], 0.3F);
                sadd(loc, x, zc - 14, x + 8, zc - 6, height, 5, Roof.GABLE_X, wall, ROOFS[(n + 2) % ROOFS.length], 0.3F);
                n++;
            }
        }
    }

    /** Равнина Голема: аметистовое кольцо, чёрная плита и восемь колонн. */
    private static void buildPlain() {
        StoryLocation p = StoryLocation.PLAIN;
        sdisc(p, 1200, 1100, 11.0D, 0xA98FD0, 0.3F);
        sdisc(p, 1200, 1100, 9.5D, 0x3D3A4C, 0.3F);
        srect(p, 1198, 1098, 1202, 1102, 0x5B3A86, 0, 0.5F);
        for (int k = 0; k < 8; k++) {
            double px = 1200 + Math.round(13.0D * Math.cos(k * Math.PI / 4.0D));
            double pz = 1100 + Math.round(13.0D * Math.sin(k * Math.PI / 4.0D));
            tower(p, Reloc.x(px, pz) + 0.5D, Reloc.z(px, pz) + 0.5D, 1.0D, 0, 6, 0, Roof.NONE, STONE, STONE_TOP, 0.5F);
        }
    }

    /** Висячие сады: парящий остров, кварцевые аллеи и ротонда дворца с золотым куполом. */
    private static void buildGardens() {
        StoryLocation g = StoryLocation.GARDENS;
        sdisc(g, 1600, 1100, 37.5D, 0x8F9A82, 0.3F);
        sdisc(g, 1600, 1100, 36.0D, 0xA9DB93, 0.3F);
        srect(g, 1600 - 35, 1100 - 1, 1600 + 35, 1100 + 1, 0xF4F1EA, 0, 0.3F);
        srect(g, 1600 - 1, 1100 - 35, 1600 + 1, 1100 + 35, 0xF4F1EA, 0, 0.3F);
        sdisc(g, 1600, 1100, 9.0D, 0xF4F1EA, 0.3F);
        tower(g, Reloc.x(1600, 1100) + 0.5D, Reloc.z(1600, 1100) + 0.5D, 8.0D, 0, 10, 7, Roof.DOME, WHITE, 0xE9C96A, 0.3F);
    }

    /** Ворота на дорогах (StoryGates): два столба с каждой стороны и перекладина. Ворота замка это сама привратная башня. */
    private static void buildGates() {
        for (StoryLocation loc : StoryLocation.values()) {
            StoryLocation prim = loc.primary();
            if (prim == StoryLocation.CASTLE_LAB) {
                continue;
            }
            for (TrifasRoads.Opening o : TrifasRoads.openings(loc)) {
                boolean alongX = Math.abs(o.ux()) >= Math.abs(o.uz());
                int gx = o.gx();
                int gz = o.gz();
                for (int side : new int[] {-4, 4}) {
                    double px = alongX ? gx : gx + side;
                    double pz = alongX ? gz + side : gz;
                    add(prim, px, pz, alongX ? px + 1 : px, alongX ? pz : pz + 1, 0, 8, 0, Roof.NONE, STONE, STONE_TOP, 0.4F);
                }
                if (alongX) {
                    add(prim, gx, gz - 4, gx + 1, gz + 4, 7, 1, 0, Roof.NONE, STONE_DARK, STONE_TOP, 0.4F);
                } else {
                    add(prim, gx - 4, gz, gx + 4, gz + 1, 7, 1, 0, Roof.NONE, STONE_DARK, STONE_TOP, 0.4F);
                }
            }
        }
    }

    // ===================== случайные постройки мира (WorldDecor) =====================
    private static final int CELL = 72;

    /**
     * Считает в фоне те же клетки с постройками, что и WorldDecor: клетка 72x72, постройка с шансом 55%, место и
     * вид зависят только от номера клетки и рельефа. Карта показывает их заранее: в мире они появятся, когда игрок
     * подойдёт близко.
     */
    private static void ensureDecor() {
        if (decorStarted) {
            return;
        }
        decorStarted = true;
        Thread t = new Thread(() -> {
            try {
                computeDecor();
            } catch (RuntimeException e) {
                decorStarted = false;
            }
        }, "fate-map-decor");
        t.setDaemon(true);
        t.start();
    }

    private static void computeDecor() {
        List<Block> blocks = new ArrayList<>();
        List<Patch> patches = new ArrayList<>();
        int i0 = Math.floorDiv((int) MapTerrain.X0, CELL);
        int i1 = Math.floorDiv((int) (MapTerrain.X0 + MapTerrain.WIDTH), CELL);
        int j0 = Math.floorDiv((int) MapTerrain.Z0, CELL);
        int j1 = Math.floorDiv((int) (MapTerrain.Z0 + MapTerrain.HEIGHT), CELL);
        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                long key = ((long) i << 32) ^ (j & 0xFFFFFFFFL);
                Random rnd = new Random(key * 0x9E3779B97F4A7C15L + 12345L);
                if (rnd.nextInt(100) >= 55) {
                    continue;
                }
                int ax = i * CELL + 12 + rnd.nextInt(CELL - 24);
                int az = j * CELL + 12 + rnd.nextInt(CELL - 24);
                if (TrifasTerrain.padDist(ax, az) < 75.0D || TrifasRoads.roadDist(ax, az) < 9.0D) {
                    continue;
                }
                int sum = 0;
                int min = 1000;
                int max = -1000;
                int n = 0;
                for (int dx = -5; dx <= 5; dx += 5) {
                    for (int dz = -5; dz <= 5; dz += 5) {
                        int h = TrifasTerrain.height(ax + dx, az + dz);
                        sum += h;
                        min = Math.min(min, h);
                        max = Math.max(max, h);
                        n++;
                    }
                }
                if (min < TrifasTerrain.SEA + 3 || max > 140 || max - min > 5) {
                    continue;
                }
                boolean nearRoad = TrifasRoads.roadDist(ax, az) < 70.0D;
                int roll = rnd.nextInt(10);
                int type;
                if (nearRoad) {
                    type = roll < 3 ? 0 : roll < 6 ? 1 : roll < 8 ? 2 : 3;
                } else {
                    type = roll < 2 ? 2 : roll < 4 ? 3 : roll < 6 ? 4 : roll < 8 ? 5 : roll < 9 ? 0 : 1;
                }
                decorPiece(blocks, patches, type, rnd, ax, az);
            }
        }
        decorPatches = patches;
        decorBlocks = blocks;
    }

    private static void decorPiece(List<Block> blocks, List<Patch> patches, int type, Random rnd, int ax, int az) {
        final float z = 0.5F;
        switch (type) {
            case 0 -> {   // дом: бревенчатая хижина
                boolean oak = rnd.nextBoolean();
                blocks.add(new Block(null, false, ax - 3, az - 2, ax + 3, az + 2, 0, 3, 3, Roof.GABLE_X,
                        oak ? 0xC29A5C : 0x7B5B3C, oak ? 0x8A6A3E : 0x5C4430, z));
            }
            case 1 -> {   // колодец
                blocks.add(new Block(null, true, ax + 0.5D, az + 0.5D, 1.7D, 0.0D, 0, 1.3D, 0, Roof.NONE,
                        0x9E9E9A, 0x7FB6E6, z));
            }
            case 2 -> {   // лагерь: палатка и костёр
                int[] wools = {0xC8453F, 0xE7E2D6, 0x8E8E96};
                int wool = wools[rnd.nextInt(3)];
                blocks.add(new Block(null, false, ax - 2, az - 2, ax + 1, az + 2, 0, 1, 3, Roof.HIP, wool, wool, z));
                blocks.add(new Block(null, true, ax + 3.5D, az + 0.5D, 0.8D, 0.0D, 0, 1, 0, Roof.NONE, 0xE8873A, 0xF7C45A, 0.8F));
            }
            case 3 -> {   // святилище: каменная площадка и колонна
                blocks.add(new Block(null, false, ax - 3, az - 3, ax + 3, az + 3, 0, 1, 0, Roof.NONE, 0xAFAFAD, 0xC9C9C4, z));
                blocks.add(new Block(null, false, ax - 1, az - 1, ax + 1, az + 1, 1, 4, 0, Roof.NONE, 0xD8D6CF, 0xECEAE4, z));
            }
            case 4 -> {   // руина: кольцо обломков стен
                patches.add(new Patch(null, true, ax + 0.5D, az + 0.5D, 5.5D, 0.0D, 0xB9B4AA, 0, z));
                for (int a = 0; a < 360; a += 36) {
                    double px = ax + Math.round(5.0D * Math.cos(Math.toRadians(a)));
                    double pz = az + Math.round(5.0D * Math.sin(Math.toRadians(a)));
                    blocks.add(new Block(null, false, px, pz, px + 1, pz, 0, 2 + (a / 36) % 3, 0, Roof.NONE,
                            0xA8B09A, 0xBFC6B2, 0.9F));
                }
            }
            default -> {   // дозорная башня
                blocks.add(new Block(null, false, ax - 2, az - 2, ax + 2, az + 2, 0, 9, 3, Roof.HIP, 0x7B5B3C, WOOD_DARK, z));
            }
        }
    }

    // ===================== рисование =====================
    private static double depth(Block b) {
        return b.round() ? b.z1() + b.x2() : b.z2();
    }

    /**
     * Рисует постройки видимой части карты. known: места, которые игрок уже открыл (постройки закрытых мест не
     * показываются, как и их метки). sx и sy переводят мировые координаты в экранные.
     */
    static void draw(GuiGraphics g, float zoom, int vx1, int vy1, int vx2, int vy2,
                     DoubleUnaryOperator sx, DoubleUnaryOperator sy, Set<StoryLocation> known) {
        ensureDecor();
        for (Patch p : PATCHES) {
            if (zoom >= p.minZoom() && (p.loc() == null || known.contains(p.loc()))) {
                drawPatch(g, p, zoom, vx1, vy1, vx2, vy2, sx, sy);
            }
        }
        for (Patch p : decorPatches) {
            if (zoom >= p.minZoom()) {
                drawPatch(g, p, zoom, vx1, vy1, vx2, vy2, sx, sy);
            }
        }

        VISIBLE.clear();
        collect(BLOCKS, zoom, vx1, vy1, vx2, vy2, sx, sy, known);
        collect(decorBlocks, zoom, vx1, vy1, vx2, vy2, sx, sy, known);
        VISIBLE.sort(DEPTH);
        // сначала всё на земле, затем приподнятое (башни и дворец на крыше корпуса): иначе корпус перекрыл бы их
        for (Block b : VISIBLE) {
            if (b.base() <= 0.0D) {
                drawBlock(g, b, zoom, sx, sy);
            }
        }
        for (Block b : VISIBLE) {
            if (b.base() > 0.0D) {
                drawBlock(g, b, zoom, sx, sy);
            }
        }
    }

    /** Есть ли у карты на этом масштабе постройки этого места (тогда точку-метку места рисовать не нужно). */
    static boolean hasArt(StoryLocation loc, float zoom) {
        if (loc == StoryLocation.CASTLE_LAB) {
            return zoom >= 0.18F;
        }
        switch (loc) {
            case FARM, RED_CAMP, CHAPEL, RUINS, STREET, LONDON, PLAIN, GARDENS:
                return zoom >= 0.55F;
            default:
                return false;
        }
    }

    private static void collect(List<Block> list, float zoom, int vx1, int vy1, int vx2, int vy2,
                                DoubleUnaryOperator sx, DoubleUnaryOperator sy, Set<StoryLocation> known) {
        for (Block b : list) {
            if (zoom < b.minZoom() || (b.loc() != null && !known.contains(b.loc()))) {
                continue;
            }
            double pad = (b.base() + b.h() + b.roofH()) * K * zoom + 4.0D;
            double ax = sx.applyAsDouble(b.round() ? b.x1() - b.x2() : b.x1());
            double bx = sx.applyAsDouble(b.round() ? b.x1() + b.x2() : b.x2() + 1);
            double ay = sy.applyAsDouble(b.round() ? b.z1() - b.x2() : b.z1());
            double by = sy.applyAsDouble(b.round() ? b.z1() + b.x2() : b.z2() + 1);
            if (bx < vx1 - 4 || ax > vx2 + 4 || by + 4 < vy1 || ay - pad > vy2) {
                continue;
            }
            VISIBLE.add(b);
        }
    }

    private static void drawPatch(GuiGraphics g, Patch p, float zoom, int vx1, int vy1, int vx2, int vy2,
                                  DoubleUnaryOperator sx, DoubleUnaryOperator sy) {
        int argb = 0xFF000000 | p.color();
        if (p.disc()) {
            int cx = round(sx.applyAsDouble(p.x1()));
            int cy = round(sy.applyAsDouble(p.z1()));
            int r = Math.max(1, round(p.x2() * zoom));
            if (cx + r < vx1 || cx - r > vx2 || cy + r < vy1 || cy - r > vy2) {
                return;
            }
            disc(g, cx, cy, r, argb);
            return;
        }
        int xa = round(sx.applyAsDouble(p.x1()));
        int xb = Math.max(xa + 1, round(sx.applyAsDouble(p.x2())));
        int ya = round(sy.applyAsDouble(p.z1()));
        int yb = Math.max(ya + 1, round(sy.applyAsDouble(p.z2())));
        if (xb < vx1 || xa > vx2 || yb < vy1 || ya > vy2) {
            return;
        }
        g.fill(xa, ya, xb, yb, argb);
        if (p.stripe() != 0 && zoom >= 0.6F) {
            int line = 0xFF000000 | p.stripe();
            int step = Math.max(3, round(2.0D * zoom));
            for (int y = ya + 1; y < yb; y += step) {
                g.fill(xa, y, xb, y + 1, line);
            }
        }
    }

    private static void drawBlock(GuiGraphics g, Block b, float zoom, DoubleUnaryOperator sx, DoubleUnaryOperator sy) {
        if (b.round()) {
            drawRound(g, b, zoom, sx, sy);
        } else {
            drawBox(g, b, zoom, sx, sy);
        }
    }

    private static void drawBox(GuiGraphics g, Block b, float zoom, DoubleUnaryOperator sx, DoubleUnaryOperator sy) {
        float k = K * zoom;
        int xa = round(sx.applyAsDouble(b.x1()));
        int xb = Math.max(xa + 1, round(sx.applyAsDouble(b.x2() + 1)));
        int ya = round(sy.applyAsDouble(b.z1()));
        int yb = Math.max(ya + 1, round(sy.applyAsDouble(b.z2() + 1)));
        int e = round(b.base() * k);
        int hp = Math.max(1, round(b.h() * k));
        int rh = round(b.roofH() * k);
        int yn = ya - e - hp;        // северный край верха на экране
        int ys = yb - e - hp;        // южный край верха
        int wallBottom = yb - e;

        // тень на земле: свет с северо-запада, тень уходит на юго-восток
        if (b.base() <= 0.0D && hp > 2) {
            int sh = Math.max(1, hp / 2);
            g.fill(xa + sh, ya + sh / 2, xb + sh, yb + sh / 2 + 1, 0x33203050);
        }

        // южная стена
        int wall = 0xFF000000 | shade(b.wall(), 0.80F);
        g.fill(xa, ys, xb, wallBottom, wall);
        if (zoom >= 1.1F && wallBottom - ys >= 8) {   // кладка
            int line = 0xFF000000 | shade(b.wall(), 0.72F);
            for (int y = ys + 3; y < wallBottom; y += 3) {
                g.fill(xa, y, xb, y + 1, line);
            }
        }
        if (zoom >= 0.9F && xb - xa >= 12 && wallBottom - ys >= 9 && b.roof() != Roof.NONE) {   // окна и дверь жилых домов
            int step = Math.max(6, round(4.0D * zoom));
            for (int x = xa + 3; x + 2 < xb - 2; x += step) {
                g.fill(x, ys + 3, x + 2, ys + 6, 0xFF5B6B88);
            }
            int mid = (xa + xb) / 2;
            g.fill(mid - 1, wallBottom - 4, mid + 1, wallBottom, 0xFF4A3828);
        }
        g.fill(xa, ys, xa + 1, wallBottom, 0x22000000);
        g.fill(xb - 1, ys, xb, wallBottom, 0x33000000);

        int top = 0xFF000000 | b.top();
        switch (b.roof()) {
            case GABLE_X -> {
                int ym = (yn + ys) / 2;
                int yr = ym - rh;
                if (yr > yn) {
                    g.fill(xa, yn, xb, yr, 0xFF000000 | shade(b.top(), 0.82F));
                }
                g.fill(xa, Math.min(yr, ys), xb, ys, top);
                g.fill(xa, yr, xb, yr + 1, 0xFF000000 | shade(b.top(), 1.18F));
                g.fill(xa, ys, xb, ys + 1, 0x44000000);
            }
            case GABLE_Z -> {
                int xm = (xa + xb) / 2;
                poly(g, new int[] {xa, xm, xm, xa}, new int[] {yn, yn - rh, ys - rh, ys}, 0xFF000000 | shade(b.top(), 1.08F));
                poly(g, new int[] {xm, xb, xb, xm}, new int[] {yn - rh, yn, ys, ys - rh}, 0xFF000000 | shade(b.top(), 0.80F));
                g.fill(xm, yn - rh, xm + 1, ys - rh, 0xFF000000 | shade(b.top(), 1.2F));
            }
            case HIP -> {
                int xm = (xa + xb) / 2;
                int apexY = (yn + ys) / 2 - rh;
                poly(g, new int[] {xa, xb, xm}, new int[] {yn, yn, apexY}, 0xFF000000 | shade(b.top(), 0.80F));
                poly(g, new int[] {xa, xm, xa}, new int[] {yn, apexY, ys}, 0xFF000000 | shade(b.top(), 1.0F));
                poly(g, new int[] {xb, xm, xb}, new int[] {yn, apexY, ys}, 0xFF000000 | shade(b.top(), 0.72F));
                poly(g, new int[] {xa, xb, xm}, new int[] {ys, ys, apexY}, 0xFF000000 | shade(b.top(), 1.10F));
            }
            case STEPPED -> {   // ступенчатая пирамида (синие крыши привратных башен)
                int steps = Math.max(2, Math.min(8, (int) Math.round(b.roofH() / 2.0D)));
                for (int s = 0; s < steps; s++) {
                    float t = s / (float) steps;
                    int ix = (int) ((xb - xa) / 2.0F * t);
                    int iz = (int) ((ys - yn) / 2.0F * t);
                    int rise = (int) (rh * t);
                    int c = 0xFF000000 | shade(b.top(), s % 2 == 0 ? 1.0F : 1.14F);
                    g.fill(xa + ix, yn + iz - rise, xb - ix, ys - iz - rise, c);
                }
                int ix = (xb - xa) / 2 - 1;
                g.fill(xa + ix, (yn + ys) / 2 - rh - 1, xb - ix, (yn + ys) / 2 - rh, 0xFFE8EEF8);
            }
            default -> {   // плоская крыша с парапетом
                g.fill(xa, yn, xb, ys, top);
                if (xb - xa > 4 && ys - yn > 4) {
                    g.fill(xa, yn, xb, yn + 1, 0xFF000000 | shade(b.top(), 1.12F));
                    g.fill(xa, ys - 1, xb, ys, 0x33000000);
                    g.fill(xa, yn, xa + 1, ys, 0xFF000000 | shade(b.top(), 1.12F));
                    g.fill(xb - 1, yn, xb, ys, 0x22000000);
                }
            }
        }
    }

    private static void drawRound(GuiGraphics g, Block b, float zoom, DoubleUnaryOperator sx, DoubleUnaryOperator sy) {
        float k = K * zoom;
        int cx = round(sx.applyAsDouble(b.x1()));
        int cy = round(sy.applyAsDouble(b.z1()));
        int r = Math.max(1, round(b.x2() * zoom));
        int e = round(b.base() * k);
        int hp = Math.max(1, round(b.h() * k));
        int rh = round(b.roofH() * k);
        int baseY = cy - e;
        int topY = baseY - hp;

        if (b.base() <= 0.0D && hp > 2) {
            disc(g, cx + hp / 2, cy + hp / 4, r, 0x33203050);
        }
        // нижний круг и цилиндр: левая половина освещена, правая в тени
        disc(g, cx, baseY, r, 0xFF000000 | shade(b.wall(), 0.70F));
        g.fill(cx - r, topY, cx, baseY, 0xFF000000 | shade(b.wall(), 0.92F));
        g.fill(cx, topY, cx + r, baseY, 0xFF000000 | shade(b.wall(), 0.74F));
        if (zoom >= 1.0F && hp >= 10) {   // бойница
            g.fill(cx - 1, topY + hp / 3, cx + 1, topY + hp / 3 + 3, 0xFF3E4660);
        }

        switch (b.roof()) {
            case CONE -> {
                disc(g, cx, topY, r, 0xFF000000 | shade(b.top(), 0.92F));
                poly(g, new int[] {cx - r, cx, cx}, new int[] {topY, topY, topY - rh}, 0xFF000000 | shade(b.top(), 1.12F));
                poly(g, new int[] {cx, cx + r, cx}, new int[] {topY, topY, topY - rh}, 0xFF000000 | shade(b.top(), 0.78F));
                g.fill(cx, topY - rh - 1, cx + 1, topY - rh + 1, 0xFFF2F4FA);
            }
            case DOME -> {
                disc(g, cx, topY, r, 0xFF000000 | shade(b.top(), 0.90F));
                int dr = Math.max(1, (int) (r * 0.85F));
                disc(g, cx, topY - rh / 2, dr, 0xFF000000 | b.top());
                disc(g, cx - dr / 3, topY - rh / 2 - dr / 3, Math.max(1, dr / 2), 0xFF000000 | shade(b.top(), 1.2F));
            }
            default -> {
                disc(g, cx, topY, r, 0xFF000000 | b.top());
                if (r >= 4) {
                    disc(g, cx, topY, r - 1, 0xFF000000 | shade(b.top(), 1.06F));
                }
            }
        }
    }

    // ===================== мелкие помощники рисования =====================
    private static int round(double v) {
        return (int) Math.round(v);
    }

    /** Умножает яркость цвета (без альфы). */
    private static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * f));
        int gr = Math.min(255, (int) (((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((rgb & 0xFF) * f));
        return (r << 16) | (gr << 8) | b;
    }

    /** Закрашенный круг построчно. */
    private static void disc(GuiGraphics g, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(Math.max(0.0D, (double) r * r - (double) dy * dy + r * 0.4D)));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, argb);
        }
    }

    /** Закрашенный выпуклый многоугольник (до 6 вершин) построчно. */
    private static void poly(GuiGraphics g, int[] xs, int[] ys, int argb) {
        int n = xs.length;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int y : ys) {
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        for (int y = minY; y < maxY; y++) {
            double yc = y + 0.5D;
            double lo = Double.MAX_VALUE;
            double hi = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double y1 = ys[i];
                double y2 = ys[j];
                if ((yc >= y1 && yc < y2) || (yc >= y2 && yc < y1)) {
                    double t = (yc - y1) / (y2 - y1);
                    double x = xs[i] + (xs[j] - xs[i]) * t;
                    lo = Math.min(lo, x);
                    hi = Math.max(hi, x);
                }
            }
            if (lo <= hi) {
                int x1 = (int) Math.floor(lo);
                g.fill(x1, y, Math.max(x1 + 1, (int) Math.ceil(hi)), y + 1, argb);
            }
        }
    }
}
