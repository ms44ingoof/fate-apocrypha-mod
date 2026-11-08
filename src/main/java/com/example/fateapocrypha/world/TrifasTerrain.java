package com.example.fateapocrypha.world;

import com.example.fateapocrypha.scene.Reloc;
import com.example.fateapocrypha.scene.StoryWorld;
import java.util.Random;

/**
 * Рельеф открытого мира Трифаса: чистая функция высоты h(x, z) без обращений к Minecraft.
 *
 * Мир: большой континент (эллипс) в океане. Горные цепи (хребты с острыми гребнями), плато-мезы с террасами,
 * реки и глубокие каньоны, пляжи и парящие острова. Под каждой сюжетной локацией рельеф плавно выравнивается
 * до пола локации (StoryWorld.Y0): площадка вплавлена в местность, вокруг неё склоны и долины.
 */
public final class TrifasTerrain {
    public static final int SEA = 63;
    public static final int FLOOR = StoryWorld.Y0;

    /** Площадки сюжетных локаций: x1, z1, x2, z2 (границы построек вместе с кольцом деревьев). */
    private static final int[][] RAW_PADS = {
            {100, 100, 190, 130}, {100, 298, 130, 306}, {100, 398, 208, 416},                       // замок
            {StoryWorld.FOREST_X0, StoryWorld.FOREST_Z0,
                    StoryWorld.FOREST_X0 + StoryWorld.FOREST_SIZE - 1, StoryWorld.FOREST_Z0 + StoryWorld.FOREST_SIZE - 1},
            {784, 784, 816, 816}, {790, 890, 810, 920}, {780, 980, 820, 1020},                      // лагерь, церковь, руины
            {1180, 786, 1220, 814}, {1175, 870, 1225, 920}, {1180, 986, 1220, 1014}, {1172, 1072, 1228, 1128},
            {1165, 1170, 1217, 1220},                                                                // ферма
            {StoryWorld.GARDEN_X - 30, StoryWorld.GARDEN_Z - 30, StoryWorld.GARDEN_X + 30, StoryWorld.GARDEN_Z + 30},
            {StoryWorld.GRAIL_X - 34, StoryWorld.GRAIL_Z - 34, StoryWorld.GRAIL_X + 34, StoryWorld.GRAIL_Z + 34},
            {StoryWorld.REVERSE_X - 34, StoryWorld.REVERSE_Z - 34, StoryWorld.REVERSE_X + 34, StoryWorld.REVERSE_Z + 34},
            {StoryWorld.DREAM_X - 34, StoryWorld.DREAM_Z - 34, StoryWorld.DREAM_X + 34, StoryWorld.DREAM_Z + 34}
    };
    /** Площадки в настоящих мировых координатах (сюжетные координаты пересчитаны по смещению локаций). */
    private static final int[][] PADS = remap(RAW_PADS);

    private static int[][] remap(int[][] raw) {
        int[][] out = new int[raw.length][];
        for (int i = 0; i < raw.length; i++) {
            out[i] = Reloc.rect(raw[i][0], raw[i][1], raw[i][2], raw[i][3]);
        }
        return out;
    }

    private static final double PAD_MARGIN = 16.0D;   // внутри этого расстояния пол строго на уровне FLOOR
    private static final double PAD_BLEND = 46.0D;    // дальше площадка плавно переходит в естественный рельеф

    // ---------- шум ----------
    private static final class Noise {
        private final int[] perm = new int[512];

        Noise(long seed) {
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) {
                p[i] = i;
            }
            Random r = new Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = r.nextInt(i + 1);
                int t = p[i];
                p[i] = p[j];
                p[j] = t;
            }
            for (int i = 0; i < 512; i++) {
                perm[i] = p[i & 255];
            }
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
        }

        private static double grad(int h, double x, double y) {
            switch (h & 7) {
                case 0: return x + y;
                case 1: return x - y;
                case 2: return -x + y;
                case 3: return -x - y;
                case 4: return x;
                case 5: return -x;
                case 6: return y;
                default: return -y;
            }
        }

        double noise(double x, double y) {
            int xi = (int) Math.floor(x);
            int yi = (int) Math.floor(y);
            double xf = x - xi;
            double yf = y - yi;
            xi &= 255;
            yi &= 255;
            double u = fade(xf);
            double v = fade(yf);
            int aa = perm[perm[xi] + yi];
            int ab = perm[perm[xi] + yi + 1];
            int ba = perm[perm[xi + 1] + yi];
            int bb = perm[perm[xi + 1] + yi + 1];
            double x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1.0D, yf), u);
            double x2 = lerp(grad(ab, xf, yf - 1.0D), grad(bb, xf - 1.0D, yf - 1.0D), u);
            return lerp(x1, x2, v) * 1.1D;
        }

        double fbm(double x, double y, int octaves) {
            double sum = 0.0D;
            double amp = 1.0D;
            double norm = 0.0D;
            double f = 1.0D;
            for (int i = 0; i < octaves; i++) {
                sum += noise(x * f, y * f) * amp;
                norm += amp;
                amp *= 0.5D;
                f *= 2.0D;
            }
            return Math.max(-1.0D, Math.min(1.0D, sum / norm));
        }
    }

    private static final Noise N_HILLS = new Noise(1101L);
    private static final Noise N_WARP = new Noise(1102L);
    private static final Noise N_RIDGE = new Noise(1103L);
    private static final Noise N_MASK = new Noise(1104L);
    private static final Noise N_MESA = new Noise(1105L);
    private static final Noise N_RIVER = new Noise(1106L);
    private static final Noise N_COAST = new Noise(1107L);
    private static final Noise N_ISLE = new Noise(1108L);
    private static final Noise N_TEMP = new Noise(1109L);
    private static final Noise N_HUM = new Noise(1110L);
    private static final Noise N_DETAIL = new Noise(1111L);

    private TrifasTerrain() {
    }

    // ---------- математика ----------
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double smooth(double a, double b, double t) {
        double x = Math.max(0.0D, Math.min(1.0D, (t - a) / (b - a)));
        return x * x * (3.0D - 2.0D * x);
    }

    private static double terrace(double h, double step) {
        double f = h / step;
        double fl = Math.floor(f);
        double fr = f - fl;
        return (fl + smooth(0.72D, 1.0D, fr)) * step;
    }

    // ---------- площадки ----------
    public static double padDist(double x, double z) {
        double best = Double.MAX_VALUE;
        for (int[] p : PADS) {
            double dx = Math.max(Math.max(p[0] - x, 0.0D), x - p[2]);
            double dz = Math.max(Math.max(p[1] - z, 0.0D), z - p[3]);
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < best) {
                best = d;
            }
        }
        return best;
    }

    /** 1 на площадке локации, плавно падает до 0 за PAD_BLEND блоков от неё. */
    public static double padWeight(double x, double z) {
        return 1.0D - smooth(PAD_MARGIN, PAD_MARGIN + PAD_BLEND, padDist(x, z));
    }

    // ---------- континент ----------
    public static double landMask(double x, double z) {
        double nx = (x - 1250.0D) / 2100.0D;
        double nz = (z - 700.0D) / 1400.0D;
        double d = Math.sqrt(nx * nx + nz * nz);
        double wob = N_COAST.fbm(x * 0.0016D, z * 0.0016D, 3) * 0.22D + N_COAST.noise(x * 0.012D + 5.0D, z * 0.012D) * 0.03D;
        return 1.0D - smooth(0.80D + wob, 1.02D + wob, d);
    }

    public static double riverWeight(double x, double z) {
        double rv = Math.abs(N_RIVER.fbm(x * 0.0016D + 300.0D, z * 0.0016D - 120.0D, 3));
        return (1.0D - smooth(0.006D, 0.030D, rv)) * smooth(0.2D, 0.6D, landMask(x, z));
    }

    public static double mesaWeight(double x, double z) {
        double pwide = 1.0D - smooth(30.0D, 210.0D, padDist(x, z));
        return smooth(0.56D, 0.74D, N_MESA.fbm(x * 0.0016D - 77.0D, z * 0.0016D + 31.0D, 2) * 0.5D + 0.5D) * (1.0D - pwide);
    }

    /** Естественный рельеф (без площадок). */
    private static double natural(double x, double z) {
        double pwide = 1.0D - smooth(30.0D, 210.0D, padDist(x, z));
        double land = landMask(x, z);

        double hills = N_HILLS.fbm(x * 0.0042D, z * 0.0042D, 4);
        double h = 77.0D - 5.0D * pwide + hills * (12.0D - 6.0D * pwide);

        // горные цепи: хребты с острыми гребнями, исказённые шумом
        double wx = x + 70.0D * N_WARP.noise(x * 0.002D, z * 0.002D);
        double wz = z + 70.0D * N_WARP.noise(x * 0.002D + 40.0D, z * 0.002D - 17.0D);
        double rn = 1.0D - Math.abs(N_RIDGE.fbm(wx * 0.0034D, wz * 0.0034D, 4));
        rn = Math.pow(Math.max(rn, 0.0D), 2.7D);
        double mm = smooth(0.40D, 0.68D, N_MASK.fbm(x * 0.0011D + 13.7D, z * 0.0011D - 9.1D, 3) * 0.5D + 0.5D);
        h += rn * 175.0D * mm * (1.0D - 0.9D * pwide);
        h += N_DETAIL.fbm(x * 0.03D, z * 0.03D, 2) * 2.5D;

        // плато-мезы со ступенчатыми склонами
        double tm = mesaWeight(x, z);
        if (tm > 0.0D) {
            h = lerp(h, terrace(h, 13.0D), tm);
        }

        // реки и каньоны
        double rw = riverWeight(x, z);
        h = lerp(h, SEA - 5.0D, rw);

        // океан: дно уходит вглубь за побережьем
        double sea = 36.0D + N_HILLS.fbm(x * 0.006D, z * 0.006D, 2) * 8.0D;
        h = lerp(sea, h, smooth(0.02D, 0.9D, land));
        return h;
    }

    /** Высота верхнего твёрдого блока в колонке (x, z). */
    public static int height(int x, int z) {
        double w = padWeight(x, z);
        double h = w >= 1.0D ? FLOOR : lerp(natural(x, z), FLOOR, w);
        return (int) Math.round(Math.max(4.0D, Math.min(300.0D, h)));
    }

    // ---------- парящие острова ----------
    public static double islandThickness(int x, int z) {
        double n = N_ISLE.fbm(x * 0.0052D + 900.0D, z * 0.0052D - 400.0D, 3) * 0.5D + 0.5D;
        double m = smooth(0.64D, 0.78D, n);
        if (m <= 0.0D) {
            return 0.0D;
        }
        m *= smooth(130.0D, 230.0D, padDist(x, z));   // не над сюжетными локациями
        return m * 46.0D;
    }

    public static int islandTop(int x, int z) {
        return (int) Math.round(176.0D + 16.0D * N_ISLE.fbm(x * 0.01D - 10.0D, z * 0.01D + 30.0D, 2));
    }

    // ---------- поверхность ----------
    public static final int KIND_GRASS = 0;
    public static final int KIND_STONE = 1;
    public static final int KIND_SNOW = 2;
    public static final int KIND_SAND = 3;
    public static final int KIND_GRAVEL = 4;
    public static final int KIND_MESA = 5;
    public static final int KIND_ROAD = 6;
    public static final int KIND_ROAD_EDGE = 7;

    /** Столбец лежит на дороге (мост через воду, тропа на суше). */
    public static boolean onRoad(int x, int z) {
        return padDist(x, z) > 0.5D && TrifasRoads.roadDist(x, z) <= 1.6D;
    }

    public static int surfaceKind(int x, int z, int h, int slope) {
        if (padDist(x, z) > 0.5D && h > SEA) {
            double rd = TrifasRoads.roadDist(x, z);
            if (rd <= 1.6D) {
                return KIND_ROAD;
            }
            if (rd <= 2.7D) {
                return KIND_ROAD_EDGE;
            }
        }
        if (padWeight(x, z) > 0.5D) {
            return KIND_GRASS;
        }
        if (h <= SEA - 1) {
            return riverWeight(x, z) > 0.25D ? KIND_GRAVEL : KIND_SAND;
        }
        if (h <= SEA + 2) {
            return KIND_SAND;
        }
        double detail = N_DETAIL.noise(x * 0.07D, z * 0.07D);
        if (h + detail * 8.0D >= 168.0D && slope < 10) {
            return KIND_SNOW;
        }
        if (mesaWeight(x, z) > 0.5D && h >= SEA + 6) {
            return KIND_MESA;
        }
        if (slope >= 6 || (h >= 140 && detail > 0.15D)) {
            return KIND_STONE;
        }
        return KIND_GRASS;
    }

    // ---------- биомы ----------
    public static String biome(int x, int z, int y) {
        if (padWeight(x, z) > 0.97D) {
            return "minecraft:the_void";
        }
        if (y >= 140 && islandThickness(x, z) > 4.0D) {
            return N_TEMP.noise(x * 0.004D, z * 0.004D) > 0.05D ? "minecraft:cherry_grove" : "minecraft:meadow";
        }
        int h = height(x, z);
        double temp = N_TEMP.fbm(x * 0.0020D, z * 0.0020D, 3);
        double hum = N_HUM.fbm(x * 0.0023D + 55.0D, z * 0.0023D - 80.0D, 3);
        if (h <= SEA - 2) {
            if (riverWeight(x, z) > 0.35D) {
                return "minecraft:river";
            }
            return h < 42 ? "minecraft:deep_ocean" : "minecraft:ocean";
        }
        if (h <= SEA + 2) {
            return "minecraft:beach";
        }
        if (h >= 172) {
            return "minecraft:jagged_peaks";
        }
        if (h >= 150) {
            return temp < 0.15D ? "minecraft:frozen_peaks" : "minecraft:stony_peaks";
        }
        if (h >= 125) {
            return temp < 0.1D ? "minecraft:grove" : "minecraft:windswept_hills";
        }
        double tm = mesaWeight(x, z);
        if (tm > 0.5D) {
            return tm > 0.8D ? "minecraft:eroded_badlands" : "minecraft:badlands";
        }
        if (temp < -0.35D) {
            return hum > 0.0D ? "minecraft:snowy_taiga" : "minecraft:snowy_plains";
        }
        if (temp < -0.05D) {
            return hum > 0.1D ? "minecraft:taiga" : hum < -0.25D ? "minecraft:meadow" : "minecraft:birch_forest";
        }
        if (temp < 0.30D) {
            if (hum > 0.30D) {
                return "minecraft:dark_forest";
            }
            if (hum > 0.0D) {
                return "minecraft:forest";
            }
            return hum > -0.25D ? "minecraft:cherry_grove" : "minecraft:plains";
        }
        if (hum > 0.15D) {
            return "minecraft:flower_forest";
        }
        return hum > -0.2D ? "minecraft:sunflower_plains" : "minecraft:savanna";
    }

    public static final String[] ALL_BIOMES = {
            "minecraft:the_void", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:river", "minecraft:beach",
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
            "minecraft:birch_forest", "minecraft:dark_forest", "minecraft:flower_forest", "minecraft:cherry_grove",
            "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:snowy_plains", "minecraft:grove",
            "minecraft:windswept_hills", "minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks",
            "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:savanna"
    };
}
