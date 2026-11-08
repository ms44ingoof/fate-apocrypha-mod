package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.story.StoryLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Пересчёт "сюжетных" координат в мировые. Постройки и сцены написаны в сюжетных координатах
 * (старых, где локации стояли рядом), а каждая локация в настоящем мире стоит на своём месте (StoryLocation.cx/cz):
 * при записи блоков и при работе сцен координаты внутри круга локации сдвигаются на её смещение.
 * Вне кругов локаций координаты не меняются.
 */
public final class Reloc {
    /** x1, z1, x2, z2 (сюжетные), dx, dz. */
    private static final int[][] REGIONS;

    static {
        StoryLocation[] all = StoryLocation.values();
        REGIONS = new int[all.length][];
        for (int i = 0; i < all.length; i++) {
            StoryLocation l = all[i];
            int r = l.radius();
            REGIONS[i] = new int[] {l.storyCx() - r, l.storyCz() - r, l.storyCx() + r, l.storyCz() + r,
                    l.cx() - l.storyCx(), l.cz() - l.storyCz()};
        }
    }

    private Reloc() {
    }

    private static int[] region(double x, double z) {
        for (int[] r : REGIONS) {
            if (x >= r[0] && x <= r[2] + 1 && z >= r[1] && z <= r[3] + 1) {
                return r;
            }
        }
        return null;
    }

    public static double x(double x, double z) {
        int[] r = region(x, z);
        return r == null ? x : x + r[4];
    }

    public static double z(double x, double z) {
        int[] r = region(x, z);
        return r == null ? z : z + r[5];
    }

    public static BlockPos pos(int x, int y, int z) {
        int[] r = region(x, z);
        return r == null ? new BlockPos(x, y, z) : new BlockPos(x + r[4], y, z + r[5]);
    }

    public static BlockPos.MutableBlockPos set(BlockPos.MutableBlockPos pos, int x, int y, int z) {
        int[] r = region(x, z);
        return r == null ? pos.set(x, y, z) : pos.set(x + r[4], y, z + r[5]);
    }

    public static Vec3 vec(Vec3 v) {
        return new Vec3(x(v.x, v.z), v.y, z(v.x, v.z));
    }

    public static AABB box(AABB b) {
        double cx = (b.minX + b.maxX) / 2.0D;
        double cz = (b.minZ + b.maxZ) / 2.0D;
        return b.move(x(cx, cz) - cx, 0.0D, z(cx, cz) - cz);
    }

    public static double[][] positions(double[][] positions) {
        double[][] out = new double[positions.length][];
        for (int i = 0; i < positions.length; i++) {
            double[] p = positions[i];
            out[i] = new double[] {x(p[0], p[2]), p[1], z(p[0], p[2])};
        }
        return out;
    }

    /** Прямоугольник (x1, z1, x2, z2) в сюжетных координатах -> в мировых (по смещению области его центра). */
    public static int[] rect(int x1, int z1, int x2, int z2) {
        int[] r = region((x1 + x2) / 2.0D, (z1 + z2) / 2.0D);
        int dx = r == null ? 0 : r[4];
        int dz = r == null ? 0 : r[5];
        return new int[] {x1 + dx, z1 + dz, x2 + dx, z2 + dz};
    }

    public static int height(ServerLevel level, int x, int z) {
        int[] r = region(x, z);
        int wx = r == null ? x : x + r[4];
        int wz = r == null ? z : z + r[5];
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
    }
}
