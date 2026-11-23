package com.example.fateapocrypha.story;

import com.example.fateapocrypha.world.TrifasRoads;
import com.example.fateapocrypha.world.TrifasTerrain;
import net.minecraft.core.BlockPos;

/**
 * Доски объявлений побочных заданий. Каждая стоит у дороги сразу за воротами сюжетной локации, снаружи её круга:
 * к ней можно подойти в любой момент, даже пока сама локация закрыта. Место считается из положения ворот
 * (TrifasRoads), поэтому сервер и карта на клиенте получают одни и те же координаты.
 */
public enum SideBoard {
    CASTLE("castle", StoryLocation.CASTLE_GATE, StoryLocation.FOREST),
    FOREST("forest", StoryLocation.FOREST, StoryLocation.CASTLE_GATE),
    FARM("farm", StoryLocation.FARM, StoryLocation.CASTLE_GATE),
    STREET("street", StoryLocation.STREET, StoryLocation.LONDON),
    LONDON("london", StoryLocation.LONDON, StoryLocation.PLAIN),
    PLAIN("plain", StoryLocation.PLAIN, StoryLocation.LONDON);

    private final String id;
    private final StoryLocation location;
    private final StoryLocation toward;
    private int[] pos;     // x, z, направление лектора (0 N, 1 E, 2 S, 3 W)

    SideBoard(String id, StoryLocation location, StoryLocation toward) {
        this.id = id;
        this.location = location;
        this.toward = toward;
    }

    public String id() {
        return id;
    }

    public StoryLocation location() {
        return location;
    }

    public String nameKey() {
        return "board.fateapocrypha." + id;
    }

    private int[] pos() {
        if (pos == null) {
            int[] p = null;
            for (TrifasRoads.Opening o : TrifasRoads.openings(location)) {
                if (o.to() == toward) {
                    // 9 блоков за воротами вдоль дороги и 6 блоков в сторону от неё. Пандус замка кончается внутри
                    // круга закрытой зоны замка (радиус 175), поэтому его доску выносим дальше, за круг
                    double along = location == StoryLocation.CASTLE_GATE ? 32.0D : 9.0D;
                    int x = (int) Math.round(o.gx() + o.ux() * along - o.uz() * 6.0D);
                    int z = (int) Math.round(o.gz() + o.uz() * along + o.ux() * 6.0D);
                    // лектор смотрит на дорогу
                    double fx = o.uz();
                    double fz = -o.ux();
                    int dir = Math.abs(fx) > Math.abs(fz) ? (fx > 0.0D ? 1 : 3) : (fz > 0.0D ? 2 : 0);
                    p = new int[] {x, z, dir};
                    break;
                }
            }
            if (p == null) {
                p = new int[] {location.cx() + location.radius() + 16, location.cz(), 3};
            }
            pos = p;
        }
        return pos;
    }

    public int x() {
        return pos()[0];
    }

    public int z() {
        return pos()[1];
    }

    /** Направление взгляда лектора: 0 север, 1 восток, 2 юг, 3 запад. */
    public int facing() {
        return pos()[2];
    }

    /** Высота блока лектора: на один блок выше поверхности рельефа. */
    public int y() {
        return TrifasTerrain.height(x(), z()) + 1;
    }

    public BlockPos blockPos() {
        return new BlockPos(x(), y(), z());
    }

    private static final int[] FX = {0, 1, 0, -1};
    private static final int[] FZ = {-1, 0, 1, 0};

    /**
     * Принадлежит ли блок самой доске: щит, два столба, навес и бочка с компостером. Каменная площадка и земля вокруг
     * не считаются, поэтому клик по полу рядом с доской её не открывает.
     */
    public boolean contains(BlockPos p) {
        int dx = p.getX() - x();
        int dz = p.getZ() - z();
        int f = facing();
        int depth = dx * FX[f] + dz * FZ[f];
        int sideX = -FZ[f];   // единичный вектор вбок: по часовой стрелке от направления взгляда
        int sideZ = FX[f];
        int side = dx * sideX + dz * sideZ;
        int dy = p.getY() - y();
        if (depth == 0 && Math.abs(side) <= 1 && (dy == 1 || dy == 2)) {
            return true;                                   // щит
        }
        if (depth == 0 && Math.abs(side) == 2 && dy >= 0 && dy <= 4) {
            return true;                                   // столбы и фонари
        }
        if (dy == 3 && Math.abs(side) <= 2 && (depth == 0 || depth == 1)) {
            return true;                                   // навес
        }
        return depth == 1 && Math.abs(side) == 2 && dy == 0;   // бочка и компостер
    }

    /** Доска, которой принадлежит этот блок, или null. */
    public static SideBoard at(BlockPos pos) {
        for (SideBoard b : values()) {
            if (Math.abs(b.x() - pos.getX()) <= 4 && Math.abs(b.z() - pos.getZ()) <= 4 && b.contains(pos)) {
                return b;
            }
        }
        return null;
    }
}
