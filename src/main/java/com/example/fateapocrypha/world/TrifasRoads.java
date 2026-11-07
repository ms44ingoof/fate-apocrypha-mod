package com.example.fateapocrypha.world;

import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.scene.StoryWorld;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Дорожная сеть между сюжетными локациями. Дороги рисуются генератором мира (поверхность "тропа"),
 * через реки идут мосты. Каждая дорога начинается у края площадки одной локации и заканчивается у края другой;
 * на границе круга локации стоят ворота (StoryGates).
 */
public final class TrifasRoads {
    /** Площадки локаций (x1, z1, x2, z2), те же, что в TrifasTerrain. */
    private static final Map<StoryLocation, int[]> RECTS = new EnumMap<>(StoryLocation.class);

    static {
        RECTS.put(StoryLocation.CASTLE_LAB, new int[] {100, 100, 190, 130});
        RECTS.put(StoryLocation.CASTLE_CORRIDOR, new int[] {100, 298, 130, 306});
        RECTS.put(StoryLocation.CASTLE_GATE, new int[] {100, 398, 208, 416});
        RECTS.put(StoryLocation.FOREST, new int[] {StoryWorld.FOREST_X0, StoryWorld.FOREST_Z0,
                StoryWorld.FOREST_X0 + StoryWorld.FOREST_SIZE - 1, StoryWorld.FOREST_Z0 + StoryWorld.FOREST_SIZE - 1});
        RECTS.put(StoryLocation.RED_CAMP, new int[] {784, 784, 816, 816});
        RECTS.put(StoryLocation.CHAPEL, new int[] {790, 890, 810, 920});
        RECTS.put(StoryLocation.RUINS, new int[] {780, 980, 820, 1020});
        RECTS.put(StoryLocation.STREET, new int[] {1180, 786, 1220, 814});
        RECTS.put(StoryLocation.SIEGE, new int[] {1175, 870, 1225, 920});
        RECTS.put(StoryLocation.LONDON, new int[] {1180, 986, 1220, 1014});
        RECTS.put(StoryLocation.PLAIN, new int[] {1172, 1072, 1228, 1128});
        RECTS.put(StoryLocation.FARM, new int[] {1165, 1170, 1217, 1220});
        RECTS.put(StoryLocation.GARDENS, new int[] {StoryWorld.GARDEN_X - 30, StoryWorld.GARDEN_Z - 30,
                StoryWorld.GARDEN_X + 30, StoryWorld.GARDEN_Z + 30});
    }

    /** Связи между локациями: по ним проложены дороги и построены ворота. */
    private static final StoryLocation[][] EDGES = {
            {StoryLocation.CASTLE_LAB, StoryLocation.CASTLE_CORRIDOR},
            {StoryLocation.CASTLE_CORRIDOR, StoryLocation.CASTLE_GATE},
            {StoryLocation.CASTLE_GATE, StoryLocation.FOREST},
            {StoryLocation.FOREST, StoryLocation.RED_CAMP},
            {StoryLocation.RED_CAMP, StoryLocation.CHAPEL},
            {StoryLocation.CHAPEL, StoryLocation.RUINS},
            {StoryLocation.RED_CAMP, StoryLocation.STREET},
            {StoryLocation.STREET, StoryLocation.SIEGE},
            {StoryLocation.SIEGE, StoryLocation.LONDON},
            {StoryLocation.LONDON, StoryLocation.PLAIN},
            {StoryLocation.PLAIN, StoryLocation.FARM},
            {StoryLocation.SIEGE, StoryLocation.GARDENS},
            {StoryLocation.RUINS, StoryLocation.LONDON}
    };

    /** Проход через кольцо деревьев и ворота: направление, край площадки (e) и граница локации (g). */
    public record Opening(StoryLocation from, StoryLocation to, double ux, double uz, int ex, int ez, int gx, int gz) {
    }

    private static final List<Opening> OPENINGS = new ArrayList<>();
    private static final List<double[]> SEGMENTS = new ArrayList<>();     // x1, z1, x2, z2
    private static final List<double[]> BOXES = new ArrayList<>();        // minX, minZ, maxX, maxZ, firstSegment, lastSegment
    private static final List<double[]> LAMPS = new ArrayList<>();        // x, z, перпендикуляр x, перпендикуляр z

    static {
        int edgeIndex = 0;
        for (StoryLocation[] edge : EDGES) {
            StoryLocation a = edge[0];
            StoryLocation b = edge[1];
            double[] ea = openingPoints(a, b);
            double[] eb = openingPoints(b, a);
            OPENINGS.add(new Opening(a, b, ea[4], ea[5], (int) ea[0], (int) ea[1], (int) ea[2], (int) ea[3]));
            OPENINGS.add(new Opening(b, a, eb[4], eb[5], (int) eb[0], (int) eb[1], (int) eb[2], (int) eb[3]));

            List<double[]> pts = new ArrayList<>();
            pts.add(new double[] {ea[0], ea[1]});
            pts.add(new double[] {ea[2], ea[3]});
            double dx = eb[2] - ea[2];
            double dz = eb[3] - ea[3];
            double len = Math.hypot(dx, dz);
            int n = Math.max(1, (int) (len / 70.0D));
            Random rnd = new Random(7000L + edgeIndex);
            double nx = -dz / Math.max(1.0D, len);
            double nz = dx / Math.max(1.0D, len);
            for (int i = 1; i < n; i++) {
                double t = i / (double) n;
                double off = (rnd.nextDouble() * 2.0D - 1.0D) * 26.0D * Math.sin(Math.PI * t);
                pts.add(new double[] {ea[2] + dx * t + nx * off, ea[3] + dz * t + nz * off});
            }
            pts.add(new double[] {eb[2], eb[3]});
            pts.add(new double[] {eb[0], eb[1]});

            int first = SEGMENTS.size();
            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            double carry = 0.0D;
            for (int i = 0; i + 1 < pts.size(); i++) {
                double[] p = pts.get(i);
                double[] q = pts.get(i + 1);
                SEGMENTS.add(new double[] {p[0], p[1], q[0], q[1]});
                minX = Math.min(minX, Math.min(p[0], q[0]));
                minZ = Math.min(minZ, Math.min(p[1], q[1]));
                maxX = Math.max(maxX, Math.max(p[0], q[0]));
                maxZ = Math.max(maxZ, Math.max(p[1], q[1]));
                // фонари вдоль дороги через каждые ~48 блоков (не у самых ворот)
                double sl = Math.hypot(q[0] - p[0], q[1] - p[1]);
                double pos = carry;
                while (pos < sl) {
                    double t = pos / Math.max(1.0D, sl);
                    double lx = p[0] + (q[0] - p[0]) * t;
                    double lz = p[1] + (q[1] - p[1]) * t;
                    double ux = (q[0] - p[0]) / Math.max(1.0D, sl);
                    double uz = (q[1] - p[1]) / Math.max(1.0D, sl);
                    if (i > 0 && i + 2 < pts.size()) {
                        LAMPS.add(new double[] {lx, lz, -uz, ux});
                    }
                    pos += 48.0D;
                }
                carry = pos - sl;
            }
            BOXES.add(new double[] {minX - 6.0D, minZ - 6.0D, maxX + 6.0D, maxZ + 6.0D, first, SEGMENTS.size()});
            edgeIndex++;
        }
    }

    private TrifasRoads() {
    }

    /** ex, ez: край площадки; gx, gz: граница локации (ворота); ux, uz: направление. */
    private static double[] openingPoints(StoryLocation from, StoryLocation to) {
        int[] r = RECTS.get(from);
        double cx = (r[0] + r[2]) / 2.0D;
        double cz = (r[1] + r[3]) / 2.0D;
        double dx = to.cx() - from.cx();
        double dz = to.cz() - from.cz();
        double len = Math.max(1.0D, Math.hypot(dx, dz));
        double ux = dx / len;
        double uz = dz / len;
        double hx = (r[2] - r[0]) / 2.0D + 1.0D;
        double hz = (r[3] - r[1]) / 2.0D + 1.0D;
        double t = Math.min(Math.abs(ux) < 1.0e-6D ? 1.0e9D : hx / Math.abs(ux), Math.abs(uz) < 1.0e-6D ? 1.0e9D : hz / Math.abs(uz));
        double gt = from.radius() - 5.0D;
        return new double[] {cx + ux * t, cz + uz * t, from.cx() + ux * gt, from.cz() + uz * gt, ux, uz};
    }

    public static List<Opening> openings(StoryLocation location) {
        List<Opening> out = new ArrayList<>();
        for (Opening o : OPENINGS) {
            if (o.from() == location) {
                out.add(o);
            }
        }
        return out;
    }

    public static List<double[]> lamps() {
        return LAMPS;
    }

    /** Расстояние до оси ближайшей дороги (очень большое, если дорог рядом нет). */
    public static double roadDist(double x, double z) {
        double best = 1.0e9D;
        for (double[] box : BOXES) {
            if (x < box[0] || x > box[2] || z < box[1] || z > box[3]) {
                continue;
            }
            for (int i = (int) box[4]; i < (int) box[5]; i++) {
                double[] s = SEGMENTS.get(i);
                double vx = s[2] - s[0];
                double vz = s[3] - s[1];
                double l2 = vx * vx + vz * vz;
                double t = l2 < 1.0e-6D ? 0.0D : Math.max(0.0D, Math.min(1.0D, ((x - s[0]) * vx + (z - s[1]) * vz) / l2));
                double px = s[0] + vx * t - x;
                double pz = s[1] + vz * t - z;
                double d = Math.sqrt(px * px + pz * pz);
                if (d < best) {
                    best = d;
                }
            }
        }
        return best;
    }
}
