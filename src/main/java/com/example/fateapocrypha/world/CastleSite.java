package com.example.fateapocrypha.world;

import com.example.fateapocrypha.scene.StoryWorld;

/**
 * Замок Иггдмилления: место, высота и сдвиги сюжетных комнат, которые находятся внутри него.
 * Замок стоит на холме (высота Y), лаборатория скрыта в подвале под главным корпусом, коридор падения стал
 * галереей корпуса, покои Хирона и путь побега построены на внутреннем дворе.
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
    public static final int GATE_DX = 36;
    public static final int GATE_DZ = -98;
    public static final int GATE_DY = Y - StoryWorld.Y0;
    // Осада: внутренняя стена с воротами и площадка перед ней стоят в западной части двора
    // (сюжетные x1180-1220, z880-904 -> мир x160-200, z258-282; между корпусом, хозяйственными постройками и фонтаном)
    public static final int SIEGE_DX = -1020;
    public static final int SIEGE_DZ = -622;
    public static final int SIEGE_DY = Y - StoryWorld.Y0;

    private CastleSite() {
    }

    /** Прямоугольник холма: внешняя стена с небольшим запасом. */
    public static int[] moundRect() {
        return new int[] {X0 - 4, Z0 - 4, X0 + W + 4, Z0 + D + 4};
    }
}
