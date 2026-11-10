package com.example.fateapocrypha.client;

import net.minecraft.client.gui.GuiGraphics;

/** Скруглённые панели, градиенты, тени и значки для интерфейса заданий (в Minecraft нет скругления из коробки). */
public final class UiDraw {
    private UiDraw() {
    }

    public static int lerp(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int cornerInset(int r, int row) {
        double dy = r - row - 0.5D;
        double dx = Math.sqrt(Math.max(0.0D, (double) r * r - dy * dy));
        return (int) Math.ceil(r - dx);
    }

    /** Скруглённый прямоугольник с вертикальным градиентом. */
    public static void gradient(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int top, int bottom) {
        int h = y2 - y1;
        int w = x2 - x1;
        if (h <= 0 || w <= 0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int inset = 0;
            if (row < r) {
                inset = cornerInset(r, row);
            } else if (row >= h - r) {
                inset = cornerInset(r, h - 1 - row);
            }
            int color = top == bottom ? top : lerp(top, bottom, (float) row / Math.max(1, h - 1));
            g.fill(x1 + inset, y1 + row, x2 - inset, y1 + row + 1, color);
        }
    }

    public static void rounded(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int color) {
        gradient(g, x1, y1, x2, y2, r, color, color);
    }

    /** Панель с рамкой в 1 пиксель и градиентной заливкой. */
    public static void outlined(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int border, int top, int bottom) {
        rounded(g, x1, y1, x2, y2, r, border);
        gradient(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), top, bottom);
    }

    /** Мягкая тень под панелью. */
    public static void shadow(GuiGraphics g, int x1, int y1, int x2, int y2, int r) {
        for (int i = 6; i >= 1; i--) {
            rounded(g, x1 - i, y1 - i + 3, x2 + i, y2 + i + 3, r + i, 0x0C000000);
        }
    }

    public static void circle(GuiGraphics g, int cx, int cy, int radius, int color) {
        rounded(g, cx - radius, cy - radius, cx + radius, cy + radius, radius, color);
    }

    /** Ромб (повёрнутый скруглённый квадрат). */
    public static void diamond(GuiGraphics g, int cx, int cy, int half, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0F));
        rounded(g, -half, -half, half, half, Math.max(1, half / 3), color);
        g.pose().popPose();
    }

    /** Галочка из нескольких пикселей. */
    public static void check(GuiGraphics g, int cx, int cy, int color) {
        int[][] pts = {{-3, 0}, {-2, 1}, {-1, 2}, {0, 1}, {1, 0}, {2, -1}, {3, -2}};
        for (int[] p : pts) {
            g.fill(cx + p[0], cy + p[1], cx + p[0] + 2, cy + p[1] + 2, color);
        }
    }

    /** Линия, плавно исчезающая к правому краю. */
    public static void fadeLine(GuiGraphics g, int x, int y, int width, int color, float startAlpha) {
        for (int i = 0; i < width; i++) {
            int a = (int) (255 * startAlpha * (1.0F - (float) i / width));
            if (a > 0) {
                g.fill(x + i, y, x + i + 1, y + 1, withAlpha(color, a));
            }
        }
    }
}
