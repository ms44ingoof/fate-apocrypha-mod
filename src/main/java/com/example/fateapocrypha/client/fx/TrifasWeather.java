package com.example.fateapocrypha.client.fx;

/**
 * Погода Трифаса: дождь по расписанию, которое зависит только от игрового времени, поэтому сервер и клиент
 * согласны без пакетов. Дождливые дни составляют около 40%; в такой день дождь идёт 3-7 тысяч тиков.
 */
public final class TrifasWeather {
    private TrifasWeather() {
    }

    private static int hash(long day, int salt) {
        long x = day * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        x ^= x >>> 30;
        x *= 0x94D049BB133111EBL;
        x ^= x >>> 27;
        return (int) (x & 0x7FFFFFFF);
    }

    private static float smooth(float x) {
        x = Math.max(0.0F, Math.min(1.0F, x));
        return x * x * (3.0F - 2.0F * x);
    }

    private static boolean rainyDay(long day) {
        return hash(day, 1) % 100 < 40;
    }

    private static long start(long day) {
        return 1500L + hash(day, 2) % 9000L;
    }

    private static long end(long day) {
        return start(day) + 3500L + hash(day, 3) % 4500L;
    }

    /** Сила дождя 0..1 (с плавным началом и концом). */
    public static float intensity(long dayTime) {
        long day = dayTime / 24000L;
        long t = dayTime % 24000L;
        if (!rainyDay(day)) {
            return 0.0F;
        }
        long s = start(day);
        long e = end(day);
        if (t < s || t > e) {
            return 0.0F;
        }
        float up = Math.min(1.0F, (t - s) / 600.0F);
        float down = Math.min(1.0F, (e - t) / 600.0F);
        float strength = 0.55F + 0.45F * ((hash(day, 4) % 100) / 100.0F);
        return smooth(Math.min(up, down)) * strength;
    }

    /** Влажность земли: 1 во время дождя и до 5000 тиков после него (лужи высыхают). */
    public static float wetness(long dayTime) {
        long day = dayTime / 24000L;
        long t = dayTime % 24000L;
        if (!rainyDay(day)) {
            return 0.0F;
        }
        long s = start(day);
        long e = end(day);
        if (t < s) {
            return 0.0F;
        }
        if (t <= e) {
            return Math.min(1.0F, (t - s) / 400.0F);
        }
        return Math.max(0.0F, 1.0F - (t - e) / 5000.0F);
    }
}
