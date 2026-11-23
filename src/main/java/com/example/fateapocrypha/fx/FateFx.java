package com.example.fateapocrypha.fx;

import com.example.fateapocrypha.network.FxPacket;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.ShakePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Серверный интерфейс собственных эффектов мода. Эффекты рисует клиент (ClientFx) без частиц Майнкрафта: магические
 * круги с рунами и гексаграммой, расходящиеся кольца, световые столбы, лучи, дуги взмахов, лучистые всплески и тряска
 * камеры. Пакет получают игроки в радиусе 96 блоков.
 */
public final class FateFx {
    public static final int CIRCLE = 0;
    public static final int RING = 1;
    public static final int PILLAR = 2;
    public static final int BEAM = 3;
    public static final int SLASH = 4;
    public static final int BURST = 5;
    public static final int HELIX = 6;
    public static final int RAIN = 7;
    public static final int SUN = 8;
    public static final int WINGS = 9;
    public static final int LIGHTNING = 10;

    /** Стили магического круга: 0 гексаграмма, 1 солнце, 2 октаграмма, 3 лотос, 4 шестерня. */
    public static final int STYLE_HEX = 0;
    public static final int STYLE_SUN = 1;
    public static final int STYLE_OCTA = 2;
    public static final int STYLE_LOTUS = 3;
    public static final int STYLE_GEAR = 4;

    public static final int GOLD = 0xFFD24A;
    public static final int CYAN = 0x7FE8FF;
    public static final int RED = 0xFF2A3A;
    public static final int BLUE = 0x7FB0FF;
    public static final int WHITE = 0xFFF6DD;
    public static final int VIOLET = 0xB58CFF;
    public static final int GREEN = 0x7BE0A0;

    private FateFx() {
    }

    public static void spawn(ServerLevel level, int kind, double x, double y, double z,
                             double ax, double ay, double az, int color, float size, int ticks) {
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(x, y, z, 96.0D, level.dimension())),
                new FxPacket(kind, x, y, z, ax, ay, az, color, size, ticks));
    }

    /** Магический круг на земле (радиус в блоках), стиль по умолчанию гексаграмма. */
    public static void circle(ServerLevel level, double x, double y, double z, int color, float radius, int ticks) {
        spawn(level, CIRCLE, x, y, z, 0.0D, 0.0D, 0.0D, color, radius, ticks);
    }

    /** Магический круг заданного стиля (STYLE_*). */
    public static void circle(ServerLevel level, double x, double y, double z, int color, float radius, int ticks, int style) {
        spawn(level, CIRCLE, x, y, z, 0.0D, style, 0.0D, color, radius, ticks);
    }

    /** Спираль света, поднимающаяся вокруг точки (height: высота, radius: радиус витка). */
    public static void helix(ServerLevel level, double x, double y, double z, int color, float height, double radius, int ticks) {
        spawn(level, HELIX, x, y, z, radius, 0.0D, 0.0D, color, height, ticks);
    }

    /** Ливень светящихся нитей в радиусе над точкой. */
    public static void rain(ServerLevel level, double x, double y, double z, int color, float radius, int ticks) {
        spawn(level, RAIN, x, y, z, 0.0D, 0.0D, 0.0D, color, radius, ticks);
    }

    /** Солнечный диск с лучами на земле. */
    public static void sun(ServerLevel level, double x, double y, double z, int color, float radius, int ticks) {
        spawn(level, SUN, x, y, z, 0.0D, 0.0D, 0.0D, color, radius, ticks);
    }

    /** Крылья света за спиной (направление взгляда dx, dz; size: размах). */
    public static void wings(ServerLevel level, double x, double y, double z, double dx, double dz, int color, float size, int ticks) {
        spawn(level, WINGS, x, y, z, dx, 0.0D, dz, color, size, ticks);
    }

    /** Молния между точками a и b (width: толщина свечения). */
    public static void lightning(ServerLevel level, Vec3 a, Vec3 b, int color, float width, int ticks) {
        spawn(level, LIGHTNING, a.x, a.y, a.z, b.x, b.y, b.z, color, width, ticks);
    }

    /** Расходящееся кольцо до заданного радиуса. */
    public static void ring(ServerLevel level, double x, double y, double z, int color, float radius, int ticks) {
        spawn(level, RING, x, y, z, 0.0D, 0.0D, 0.0D, color, radius, ticks);
    }

    /** Световой столб вверх на заданную высоту. */
    public static void pillar(ServerLevel level, double x, double y, double z, int color, float height, int ticks) {
        spawn(level, PILLAR, x, y, z, 0.0D, 0.0D, 0.0D, color, height, ticks);
    }

    /** Луч из точки a в точку b (width: толщина свечения в блоках). */
    public static void beam(ServerLevel level, Vec3 a, Vec3 b, int color, float width, int ticks) {
        spawn(level, BEAM, a.x, a.y, a.z, b.x, b.y, b.z, color, width, ticks);
    }

    /** Дуга взмаха в 150 градусов перед точкой в направлении (dx, dz). */
    public static void slash(ServerLevel level, double x, double y, double z, double dx, double dz, int color,
                             float radius, int ticks) {
        spawn(level, SLASH, x, y, z, dx, 0.0D, dz, color, radius, ticks);
    }

    /** Лучистый всплеск во все стороны. */
    public static void burst(ServerLevel level, double x, double y, double z, int color, float size, int ticks) {
        spawn(level, BURST, x, y, z, 0.0D, 0.0D, 0.0D, color, size, ticks);
    }

    /** Тряска камеры у игроков рядом: сила убывает с расстоянием до range блоков. */
    public static void shake(ServerLevel level, double x, double y, double z, double range, float power, int ticks) {
        for (ServerPlayer p : level.players()) {
            double d = Math.sqrt(p.distanceToSqr(x, y, z));
            if (d < range) {
                float k = power * (float) (1.0D - d / range);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new ShakePacket(k, ticks));
            }
        }
    }
}
