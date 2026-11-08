package com.example.fateapocrypha.story;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Сюжетные локации открытого мира (круги на карте). Пока глава сюжета не ведёт в локацию, войти в неё нельзя:
 * игрока мягко выталкивает за границу. Вход в нужную локацию запускает сцену главы.
 */
public enum StoryLocation {
    // id, сюжетный центр (где построено/написано в коде), радиус, настоящий центр в мире
    CASTLE_LAB("castle_lab", 145, 115, 60, 145, 115),
    CASTLE_CORRIDOR("castle_corridor", 115, 302, 34, 115, 302),
    CASTLE_GATE("castle_gate", 155, 407, 62, 260, 600),
    FOREST("forest", 580, 430, 118, 700, 700),
    RED_CAMP("red_camp", 800, 800, 42, 1150, 450),
    CHAPEL("chapel", 800, 905, 40, 1500, 250),
    RUINS("ruins", 800, 1000, 44, 1000, 1150),
    STREET("street", 1200, 800, 42, 1450, 700),
    SIEGE("siege", 1200, 895, 44, 1850, 800),
    LONDON("london", 1200, 1000, 36, 1550, 1050),
    PLAIN("plain", 1200, 1100, 56, 1250, 1400),
    FARM("farm", 1191, 1195, 38, 500, 1100),
    GARDENS("gardens", 1600, 1100, 52, 2300, 1000),
    GRAIL("grail", 2000, 1100, 42, 2000, 1100),
    REVERSE("reverse", 2000, 1300, 42, 2000, 1300),
    DREAM("dream", 2400, 1100, 42, 2000, 1500);

    private static final double MAP_CX = 1250.0D;
    private static final double MAP_CZ = 700.0D;

    private final String id;
    private final int storyCx;
    private final int storyCz;
    private final int radius;
    private final int cx;
    private final int cz;

    StoryLocation(String id, int storyCx, int storyCz, int radius, int cx, int cz) {
        this.id = id;
        this.storyCx = storyCx;
        this.storyCz = storyCz;
        this.radius = radius;
        this.cx = cx;
        this.cz = cz;
    }

    public int storyCx() {
        return storyCx;
    }

    public int storyCz() {
        return storyCz;
    }

    public String nameKey() {
        return "location.fateapocrypha." + id;
    }

    /** Центр локации в настоящем мире. */
    public int cx() {
        return cx;
    }

    public int cz() {
        return cz;
    }

    public int radius() {
        return radius;
    }

    public double distanceTo(double x, double z) {
        return Math.hypot(x - cx, z - cz);
    }

    public boolean contains(double x, double z) {
        return distanceTo(x, z) <= radius;
    }

    public static StoryLocation at(double x, double z) {
        for (StoryLocation location : values()) {
            if (location.contains(x, z)) {
                return location;
            }
        }
        return null;
    }

    /** Локация, куда ведёт глава (null: сцена идёт сразу; так играются интерлюдии, сны и вступление). */
    public static StoryLocation forChapter(StoryChapter chapter) {
        switch (chapter) {
            case FOREST:
                return FOREST;
            case FARM:
                return FARM;
            case FRANKEN_LEGACY:
                return RUINS;
            case RESISTANCE:
            case NIGHT_TALK:
                return SIEGE;
            case CELENIKE:
                return CASTLE_GATE;
            case JACK:
                return STREET;
            case GOLEM_ADAM:
                return PLAIN;
            case GARDENS_ASSAULT:
            case KARNA:
            case SHIRO_FINAL:
                return GARDENS;
            default:
                return null;
        }
    }

    /** Телепортирует игрока к границе локации со стороны центра карты (у воды не выше поверхности). */
    public void teleportNear(ServerPlayer player, ServerLevel level) {
        double dx = MAP_CX - cx;
        double dz = MAP_CZ - cz;
        double len = Math.max(1.0D, Math.hypot(dx, dz));
        double tx = cx + dx / len * (radius + 14);
        double tz = cz + dz / len * (radius + 14);
        int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) tx, (int) tz);
        player.stopRiding();
        player.teleportTo(level, tx + 0.5D, ty, tz + 0.5D, player.getYRot(), 0.0F);
    }
}
