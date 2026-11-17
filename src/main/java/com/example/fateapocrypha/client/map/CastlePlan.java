package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.world.CastleSite;
import java.util.ArrayList;
import java.util.List;

/**
 * Планы этажей замка Иггдмилления для карты: подземелье, подвал, первый и второй этажи, крыша.
 * Координаты совпадают с постройкой (CastleBuilder, CastleBasement, CastleInterior и подземелье Хирона).
 * Тайная комната подвала на плане не показана.
 */
final class CastlePlan {
    enum Kind { ROOM, STAIRS, YARD, CIRCLE }

    record Room(String id, Kind kind, double x1, double z1, double x2, double z2, int color) {
        /** Для CIRCLE: x1 и z1 центр, x2 радиус. */
        boolean contains(double x, double z) {
            if (kind == Kind.CIRCLE) {
                return Math.hypot(x - x1, z - z1) <= x2;
            }
            return x >= x1 && x <= x2 + 1 && z >= z1 && z <= z2 + 1;
        }
    }

    static final int FLOORS = 5;
    static final String[] FLOOR_KEYS = {"dungeon", "basement", "ground", "upper", "roof"};

    /** Граница замка (внешняя стена): x, z, ширина, глубина. */
    static final double BX = CastleSite.X0;
    static final double BZ = CastleSite.Z0;
    static final double BW = CastleSite.W;
    static final double BD = CastleSite.D;

    private static final List<List<Room>> PLANS = new ArrayList<>();

    private static final int PINK = 0xF9D9E4;
    private static final int BLUE = 0xD9E8F9;
    private static final int GREEN = 0xDDF3DD;
    private static final int YELLOW = 0xFFF1C9;
    private static final int LILAC = 0xE6DCF7;
    private static final int PEACH = 0xFADFC9;
    private static final int AQUA = 0xD5F0EF;
    private static final int BEIGE = 0xEADFD3;
    private static final int GREY = 0xE3E0E8;
    private static final int STAIR = 0xCFC4DA;

    private static void add(List<Room> l, String id, Kind k, double x1, double z1, double x2, double z2, int color) {
        l.add(new Room(id, k, x1, z1, x2, z2, color));
    }

    private static void room(List<Room> l, String id, double x1, double z1, double x2, double z2, int color) {
        add(l, id, Kind.ROOM, x1, z1, x2, z2, color);
    }

    private static void stairs(List<Room> l, String id, double x1, double z1, double x2, double z2) {
        add(l, id, Kind.STAIRS, x1, z1, x2, z2, STAIR);
    }

    static {
        int cx = CastleSite.CX;
        // ---- 0: подземелье ----
        List<Room> d = new ArrayList<>();
        room(d, "lab", 165, 175, 195, 205, AQUA);
        room(d, "lab_passage", 195, 188, 235, 192, GREY);
        room(d, "capsule", 235, 180, 255, 200, GREEN);
        room(d, "chiron_room", 145, 264, 159, 278, YELLOW);
        room(d, "escape_route", 159, 269, 205, 273, GREY);
        room(d, "pillar_hall", 205, 262, 223, 280, PINK);
        room(d, "gate_corridor", 223, 269, 236, 273, GREY);
        stairs(d, "dungeon_exit", 237, 268, 254, 274);
        PLANS.add(d);
        // ---- 1: подвал ----
        List<Room> b = new ArrayList<>();
        room(b, "corridor", 164, 225, 266, 227, GREY);
        for (int n = 0; n < 4; n++) {
            room(b, "cell", 165 + 6 * n, 218, 169 + 6 * n, 223, BEIGE);
            room(b, "cell", 165 + 6 * n, 229, 169 + 6 * n, 234, BEIGE);
        }
        room(b, "wine_cellar", 192, 212, 222, 223, PEACH);
        room(b, "kitchen_cellar", 226, 212, 264, 223, YELLOW);
        room(b, "storage", 192, 229, 222, 243, BLUE);
        room(b, "alchemy_cellar", 226, 229, 264, 243, LILAC);
        room(b, "yard_tunnel", 203, 244, 205, 252, GREY);
        stairs(b, "cellar_hatch", 203, 253, 205, 258);
        stairs(b, "kitchen_stairs", 262, 218, 264, 223);
        PLANS.add(b);
        // ---- 2: первый этаж и двор ----
        List<Room> g = new ArrayList<>();
        add(g, "courtyard", Kind.YARD, BX + 4, BZ + 4, BX + BW - 4, BZ + BD - 4, 0xE9F5E1);
        room(g, "keep_gallery", 163, 172, 267, 178, GREY);
        room(g, "great_hall", 186, 181, 244, 243, PINK);
        room(g, "library", 163, 181, 183, 244, BLUE);
        room(g, "feast_hall", 247, 181, 267, 244, PEACH);
        stairs(g, "stairs_up", 163, 226, 165, 241);
        stairs(g, "stairs_up", 265, 226, 267, 241);
        stairs(g, "grand_stairs", 200, 217, 230, 232);
        stairs(g, "kitchen_stairs", 262, 218, 264, 223);
        room(g, "smithy", 138, 255, 150, 266, GREY);
        room(g, "armory", 280, 255, 292, 266, GREY);
        room(g, "tavern", 138, 326, 150, 337, YELLOW);
        room(g, "alchemy_hut", 280, 326, 292, 337, LILAC);
        room(g, "garden", 139, 274, 158, 292, GREEN);
        room(g, "stable", 272, 272, 292, 294, BEIGE);
        add(g, "fountain", Kind.CIRCLE, cx + 0.5, 272.5, 6.5, 0, 0xCFE6F7);
        room(g, "gate", cx - 15, 342, cx + 15, 352, GREY);
        stairs(g, "cellar_hatch", 203, 253, 205, 258);
        stairs(g, "dungeon_exit", 237, 268, 254, 274);
        PLANS.add(g);
        // ---- 3: второй этаж ----
        List<Room> u = new ArrayList<>();
        String[] west = {"bedroom", "study", "barracks", "lounge", "storeroom"};
        String[] east = {"map_room", "council", "infirmary", "music_room", "laundry"};
        int[] colors = {PINK, BLUE, GREY, YELLOW, BEIGE};
        int[] colors2 = {GREEN, PEACH, AQUA, LILAC, BEIGE};
        for (int i = 0; i < CastleSite.ROOMS.length; i++) {
            room(u, west[i], 163, CastleSite.ROOMS[i][0], 183, CastleSite.ROOMS[i][1], colors[i]);
            room(u, east[i], 247, CastleSite.ROOMS[i][0], 267, CastleSite.ROOMS[i][1], colors2[i]);
        }
        room(u, "balcony", 191, 190, 239, 208, PINK);
        room(u, "bridge", 184, 197, 190, 199, GREY);
        room(u, "bridge", 240, 197, 246, 199, GREY);
        stairs(u, "stairs_up", 163, 226, 165, 228);
        stairs(u, "stairs_up", 265, 226, 267, 228);
        stairs(u, "roof_stairs", 163, 187, 165, 193);
        stairs(u, "roof_stairs", 265, 187, 267, 193);
        PLANS.add(u);
        // ---- 4: крыша ----
        List<Room> r = new ArrayList<>();
        room(r, "roof_terrace", 160, 168, 270, 248, BEIGE);
        room(r, "throne_room", 190, 182, 240, 208, YELLOW);
        room(r, "belfry", 176, 199, 180, 201, PEACH);
        room(r, "chimney", 172, 242, 174, 244, GREY);
        room(r, "chimney", 256, 242, 258, 244, GREY);
        stairs(r, "roof_stairs", 163, 187, 165, 193);
        stairs(r, "roof_stairs", 265, 187, 267, 193);
        PLANS.add(r);
    }

    static List<Room> floor(int index) {
        return PLANS.get(Math.max(0, Math.min(FLOORS - 1, index)));
    }

    /** Какому этажу соответствует высота игрока. */
    static int floorOf(double y) {
        int base = CastleSite.Y;
        if (y < base - 8) {
            return 0;
        }
        if (y < base) {
            return 1;
        }
        if (y < base + 15.5D) {
            return 2;
        }
        if (y < base + 21.5D) {
            return 3;
        }
        return 4;
    }

    static boolean insideCastle(double x, double z) {
        return x >= BX && x <= BX + BW && z >= BZ && z <= BZ + BD;
    }
}
