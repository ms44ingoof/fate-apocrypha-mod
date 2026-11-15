package com.example.fateapocrypha.world;

import com.example.fateapocrypha.scene.StoryWorld;

/**
 * Замок Иггдмилления: место, высота и сдвиги сюжетных комнат, которые находятся внутри него.
 * Замок стоит на холме (высота Y), лаборатория скрыта в подвале под главным корпусом, коридор падения стал
 * галереей корпуса, покои Хирона и путь побега в подземелье под двором, осада у главных ворот.
 */
public final class CastleSite {
    public static final int X0 = 130;          // северо-западный угол внешней стены
    public static final int Z0 = 150;
    public static final int W = 170;           // ширина (по x) и глубина (по z) внешней стены
    public static final int D = 200;
    public static final int Y = StoryWorld.Y0 + 22;   // уровень двора и пола первого этажа (вершина холма)
    public static final int CX = X0 + W / 2;
    public static final int CZ = Z0 + D / 2;
    public static final int GATE_X = CX;       // ворота в южной стене
    public static final int RAMP_Z = Z0 + D + 50;     // конец пандуса перед воротами

    // Корпус (главное здание)
    public static final int KX1 = 160;
    public static final int KX2 = 270;
    public static final int KZ1 = 168;
    public static final int KZ2 = 248;

    // Смещения сюжетных областей: (сюжетные координаты) + (dx, dz, dy) = мир
    public static final int LAB_DX = 65;
    public static final int LAB_DZ = 75;
    public static final int LAB_DY = Y - 13 - StoryWorld.Y0;           // лаборатория в подвале
    public static final int GALLERY_DX = 100;
    public static final int GALLERY_DZ = -127;
    public static final int GALLERY_DY = Y - StoryWorld.Y0;
    // Покои Хирона и путь побега: подземелье под южной частью двора (пол на 13 блоков ниже двора, как у лаборатории)
    public static final int GATE_DX = 45;
    public static final int GATE_DZ = -136;
    public static final int GATE_DY = Y - 13 - StoryWorld.Y0;
    // Осада: у главных ворот. Стена с воротами это привратная башня замка (сюжетные z899-909 = мировые z342-352),
    // площадка перед ней южный двор (сюжетные x1180-1220, z880-899 = мир x195-235, z323-342)
    public static final int SIEGE_DX = -985;
    public static final int SIEGE_DZ = -557;
    public static final int SIEGE_DY = Y - StoryWorld.Y0;

    // Крылья корпуса: второй этаж и комнаты (границы по z; перегородки на 195, 207, 219, 231)
    public static final int F2 = Y + 16;               // пол второго этажа (совпадает с балконом большого зала)
    public static final int[][] ROOMS = {{184, 194}, {196, 206}, {208, 218}, {220, 230}, {232, 243}};

    private CastleSite() {
    }

    /** Прямоугольник холма: внешняя стена с небольшим запасом. */
    public static int[] moundRect() {
        return new int[] {X0 - 4, Z0 - 4, X0 + W + 4, Z0 + D + 4};
    }
}
