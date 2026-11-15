package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.world.TrifasTerrain;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Рисует рельеф Трифаса в пастельных тонах прямо из функции высот генератора мира (карта не зависит от загруженных чанков).
 * Результат состоит из двух слоёв: цветной (суша, вода, мягкие тени склонов) и слоя линий (берег и горизонтали),
 * который рисуется белым с прозрачностью и окрашивается при показе в выбранный игроком цвет контура.
 */
final class MapTerrain {
    /** Границы карты в блоках: вся суша и немного океана. */
    static final double X0 = -700.0D;
    static final double Z0 = -600.0D;
    static final double WIDTH = 3900.0D;
    static final double HEIGHT = 2600.0D;

    private static final int SEA = TrifasTerrain.SEA;

    private static final Map<String, Integer> BIOME_COLORS = new HashMap<>();

    static {
        put("minecraft:the_void", 0xD6EFC9);
        put("minecraft:plains", 0xCDEEBE);
        put("minecraft:sunflower_plains", 0xF2EFB5);
        put("minecraft:meadow", 0xC9ECCB);
        put("minecraft:forest", 0xB4E0A8);
        put("minecraft:birch_forest", 0xC6E9B6);
        put("minecraft:dark_forest", 0x9CD29B);
        put("minecraft:flower_forest", 0xF3CFE6);
        put("minecraft:cherry_grove", 0xF8D6E4);
        put("minecraft:taiga", 0xADDCC1);
        put("minecraft:snowy_taiga", 0xDDEFEF);
        put("minecraft:snowy_plains", 0xF3F8FF);
        put("minecraft:grove", 0xCFE4D8);
        put("minecraft:windswept_hills", 0xC9D4C6);
        put("minecraft:jagged_peaks", 0xE9E4EF);
        put("minecraft:frozen_peaks", 0xF2F6FC);
        put("minecraft:stony_peaks", 0xDAD4DE);
        put("minecraft:badlands", 0xF6D0B0);
        put("minecraft:eroded_badlands", 0xF2C0A2);
        put("minecraft:savanna", 0xE8E6AB);
        put("minecraft:beach", 0xFBEFDD);
        put("minecraft:river", 0xBFE4F5);
    }

    private static void put(String id, int rgb) {
        BIOME_COLORS.put(id, rgb);
    }

    /** Результат: ARGB-цвета, маска берега и маска горизонталей (альфа 0-255; rgb не используется). */
    record Result(int width, int height, int[] color, int[] coast, int[] contours, double x0, double z0, double bpp) {
    }

    private MapTerrain() {
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /**
     * Строит прямоугольник карты: (x0, z0) это мир-координата левого верхнего угла, bpp блоков на пиксель.
     * progress получает число готовых строк (для полосы загрузки), stamp/currentStamp позволяют бросить устаревший расчёт.
     */
    static Result generate(double x0, double z0, double bpp, int w, int h, boolean coarse,
                           AtomicInteger progress, AtomicInteger currentStamp, int stamp) {
        int gw = w + 2;
        int gh = h + 2;
        int[] hs = new int[gw * gh];
        IntStream.range(0, gh).parallel().forEach(j -> {
            if (currentStamp.get() != stamp) {
                return;
            }
            double wz = z0 + (j - 1) * bpp;
            for (int i = 0; i < gw; i++) {
                hs[j * gw + i] = TrifasTerrain.height((int) Math.round(x0 + (i - 1) * bpp), (int) Math.round(wz));
            }
        });

        int[] color = new int[w * h];
        int[] coastLines = new int[w * h];
        int[] contourLines = new int[w * h];
        int contourStep = coarse ? 24 : 8;
        int majorStep = 24;
        float slopeScale = (float) (1.0D / Math.max(1.0D, bpp));
        IntStream.range(0, h).parallel().forEach(j -> {
            if (currentStamp.get() != stamp) {
                return;
            }
            for (int i = 0; i < w; i++) {
                int idx = j * gw + i;
                int c = (j + 1) * gw + (i + 1);
                int ht = hs[c];
                int wx = (int) Math.round(x0 + i * bpp);
                int wz = (int) Math.round(z0 + j * bpp);
                int rgb;
                if (ht <= SEA - 1) {
                    float depth = Math.max(0.0F, Math.min(1.0F, (SEA - ht) / 30.0F));
                    rgb = mix(0xD7EEF9, 0x9FC0EE, depth);
                    if (ht <= SEA - 2 && TrifasTerrain.riverWeight(wx, wz) > 0.35D && ht > SEA - 12) {
                        rgb = mix(rgb, 0xBFE4F5, 0.6F);
                    }
                } else {
                    String biome = TrifasTerrain.biome(wx, wz, 0);
                    Integer base = BIOME_COLORS.get(biome);
                    rgb = base == null ? 0xCDEEBE : base;
                    // мягкая светотень: свет с северо-запада, тени лавандовые
                    float lit = (hs[idx + gw * 2 + 2] - hs[idx]) * slopeScale * 0.45F;
                    lit = Math.max(-0.5F, Math.min(0.5F, lit));
                    if (lit > 0.0F) {
                        rgb = mix(rgb, 0xFFFFFF, lit * 0.7F);
                    } else {
                        rgb = mix(rgb, 0x8E7FC0, -lit * 0.75F);
                    }
                    rgb = mix(rgb, 0xFFFFFF, Math.max(0.0F, Math.min(0.30F, (ht - 110) / 200.0F)));
                }
                color[j * w + i] = 0xFF000000 | rgb;

                // линии: берег и горизонтали (сравниваем с соседями справа и снизу)
                int right = hs[c + 1];
                int down = hs[c + gw];
                boolean landHere = ht > SEA - 1;
                boolean coast = landHere != (right > SEA - 1) || landHere != (down > SEA - 1);
                int alpha = 0;
                if (coast) {
                    coastLines[j * w + i] = 255;
                } else if (landHere) {
                    if (Math.floorDiv(ht, majorStep) != Math.floorDiv(right, majorStep)
                            || Math.floorDiv(ht, majorStep) != Math.floorDiv(down, majorStep)) {
                        alpha = coarse ? 95 : 125;
                    } else if (!coarse && (Math.floorDiv(ht, contourStep) != Math.floorDiv(right, contourStep)
                            || Math.floorDiv(ht, contourStep) != Math.floorDiv(down, contourStep))) {
                        alpha = 60;
                    }
                }
                contourLines[j * w + i] = alpha;
            }
            if (progress != null) {
                progress.incrementAndGet();
            }
        });

        // берег потолще на крупном плане карты: расширяем береговую линию на соседний пиксель
        if (coarse) {
            int[] copy = coastLines.clone();
            for (int j = 0; j < h - 1; j++) {
                for (int i = 0; i < w - 1; i++) {
                    if (copy[j * w + i] == 255) {
                        coastLines[j * w + i + 1] = 255;
                        coastLines[(j + 1) * w + i] = 255;
                    }
                }
            }
        }
        return new Result(w, h, color, coastLines, contourLines, x0, z0, bpp);
    }
}
