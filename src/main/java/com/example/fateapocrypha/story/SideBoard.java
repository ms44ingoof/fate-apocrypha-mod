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
    CAMP("camp", StoryLocation.RED_CAMP, StoryLocation.STREET),
    CHAPEL("chapel", StoryLocation.CHAPEL, StoryLocation.RED_CAMP),
    STREET("street", StoryLocation.STREET, StoryLocation.LONDON),
    LONDON("london", StoryLocation.LONDON, StoryLocation.PLAIN),
    RUINS("ruins", StoryLocation.RUINS, StoryLocation.LONDON);

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
                    // 9 блоков за воротами вдоль дороги и 6 блоков в сторону от неё
                    int x = (int) Math.round(o.gx() + o.ux() * 9.0D - o.uz() * 6.0D);
                    int z = (int) Math.round(o.gz() + o.uz() * 9.0D + o.ux() * 6.0D);
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

    /** Доска в этой точке мира или null. */
    public static SideBoard at(BlockPos pos) {
        for (SideBoard b : values()) {
            if (b.x() == pos.getX() && b.z() == pos.getZ() && Math.abs(b.y() - pos.getY()) <= 2) {
                return b;
            }
        }
        return null;
    }
}
