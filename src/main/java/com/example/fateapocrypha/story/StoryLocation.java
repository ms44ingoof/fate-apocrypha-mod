package com.example.fateapocrypha.story;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Сюжетные локации открытого мира (круги на карте). Пока глава сюжета не ведёт в локацию, войти в неё нельзя:
 * игрока мягко выталкивает за границу. Вход в нужную локацию запускает сцену главы.
 */
public enum StoryLocation {
    CASTLE_LAB("castle_lab", 145, 115, 60),
    CASTLE_CORRIDOR("castle_corridor", 115, 302, 34),
    CASTLE_GATE("castle_gate", 155, 407, 62),
    FOREST("forest", 580, 430, 118),
    RED_CAMP("red_camp", 800, 800, 42),
    CHAPEL("chapel", 800, 905, 40),
    RUINS("ruins", 800, 1000, 44),
    STREET("street", 1200, 800, 42),
    SIEGE("siege", 1200, 895, 44),
    LONDON("london", 1200, 1000, 36),
    PLAIN("plain", 1200, 1100, 56),
    FARM("farm", 1191, 1195, 38),
    GARDENS("gardens", 1600, 1100, 52),
    GRAIL("grail", 2000, 1100, 42),
    REVERSE("reverse", 2000, 1300, 42),
    DREAM("dream", 2400, 1100, 42);

    private static final double MAP_CX = 1250.0D;
    private static final double MAP_CZ = 700.0D;

    private final String id;
    private final int cx;
    private final int cz;
    private final int radius;

    StoryLocation(String id, int cx, int cz, int radius) {
        this.id = id;
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
    }

    public String nameKey() {
        return "location.fateapocrypha." + id;
    }

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

    /** Локация, куда ведёт глава (null: сцена идёт сразу, без прихода в локацию). */
    public static StoryLocation forChapter(StoryChapter chapter) {
        switch (chapter) {
            case FOREST:
                return FOREST;
            case FARM:
                return FARM;
            case INTERLUDE_RED:
                return RED_CAMP;
            case INTERLUDE_SHIRO:
                return CHAPEL;
            case INTERLUDE_FRANKENSTEIN:
            case FRANKEN_LEGACY:
            case INTERLUDE_DARNIC:
                return RUINS;
            case INTERLUDE_SPARTACUS:
            case RESISTANCE:
            case NIGHT_TALK:
                return SIEGE;
            case CELENIKE:
                return CASTLE_GATE;
            case JACK:
                return STREET;
            case INTERLUDE_AVICEBRON:
            case GOLEM_ADAM:
                return PLAIN;
            case INTERLUDE_GRAIL:
            case GARDENS_ASSAULT:
            case INTERLUDE_ACHILLES:
            case KARNA:
            case INTERLUDE_SEMIRAMIS:
            case INTERLUDE_PUCELLE:
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
