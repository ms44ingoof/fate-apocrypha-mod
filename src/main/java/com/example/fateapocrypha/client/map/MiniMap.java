package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.client.ClientStoryState;
import com.example.fateapocrypha.client.UiDraw;
import com.example.fateapocrypha.story.StoryLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

/** Мини-карта в углу экрана: север сверху, берег и места, свои метки, стрелка к заданию и пульсирующий круг игрока. */
public final class MiniMap {
    private static final int SIZE = 104;
    private static final double BLOCKS_PER_PX = 1.0D;   // один блок на пиксель: видны отдельные дома, деревья и дороги
    private static final int UNEXPLORED = 0xFF10131A;

    private MiniMap() {
    }

    public static void render(GuiGraphics g, int sw, int sh, float partial) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || mc.options.hideGui
                || !mc.level.dimension().equals(ModDimensions.TRIFAS) || ClientStoryState.isCinematic()) {
            return;
        }
        MapSettings.load();
        if (!MapSettings.minimap) {
            return;
        }
        int margin = 8;
        int corner = MapSettings.minimapCorner;
        int x1 = (corner & 1) == 0 ? margin : sw - SIZE - margin;
        int y1 = corner < 2 ? margin + (corner == 1 ? 14 : 0) : sh - SIZE - margin - 14;
        int x2 = x1 + SIZE;
        int y2 = y1 + SIZE;
        double px = player.getX();
        double pz = player.getZ();
        long time = Util.getMillis();

        UiDraw.shadow(g, x1 - 2, y1 - 2, x2 + 2, y2 + 2, 6);
        g.fill(x1, y1, x2, y2, UNEXPLORED);
        g.enableScissor(x1, y1, x2, y2);
        final int fx1 = x1;
        final int fy1 = y1;
        java.util.function.DoubleUnaryOperator sxm = wx -> fx1 + SIZE / 2.0D + (wx - px) / BLOCKS_PER_PX;
        java.util.function.DoubleUnaryOperator sym = wz -> fy1 + SIZE / 2.0D + (wz - pz) / BLOCKS_PER_PX;
        ChunkMap.draw(g, (float) (1.0D / BLOCKS_PER_PX), x1, y1, x2, y2, sxm, sym);

        // места
        for (MapPlaces.Place place : MapPlaces.discovered(mc)) {
            int x = (int) (x1 + SIZE / 2.0D + (place.x() - px) / BLOCKS_PER_PX);
            int y = (int) (y1 + SIZE / 2.0D + (place.z() - pz) / BLOCKS_PER_PX);
            if (x > x1 && x < x2 && y > y1 && y < y2) {
                UiDraw.circle(g, x, y, 3, 0xFFFFFFFF);
                UiDraw.circle(g, x, y, 2, 0xFF000000 | place.color());
            }
        }
        for (MapMarks.Mark m : MapMarks.list(mc)) {
            int x = (int) (x1 + SIZE / 2.0D + (m.x - px) / BLOCKS_PER_PX);
            int y = (int) (y1 + SIZE / 2.0D + (m.z - pz) / BLOCKS_PER_PX);
            if (x > x1 + 4 && x < x2 - 4 && y > y1 + 4 && y < y2 - 4) {
                MapMarks.drawIcon(g, m.icon, x, y, m.color);
            }
        }
        // стрелка к текущему заданию на краю
        StoryLocation quest = MapPlaces.quest();
        if (quest != null) {
            double dx = (quest.cx() - px) / BLOCKS_PER_PX;
            double dz = (quest.cz() - pz) / BLOCKS_PER_PX;
            double half = SIZE / 2.0D - 7.0D;
            double k = Math.max(Math.abs(dx), Math.abs(dz));
            if (k > half) {
                dx = dx / k * half;
                dz = dz / k * half;
            }
            int qx = (int) (x1 + SIZE / 2.0D + dx);
            int qy = (int) (y1 + SIZE / 2.0D + dz);
            int bob = (int) Math.round(Math.sin(time / 240.0D) * 1.5D);
            UiDraw.diamond(g, qx, qy + bob, 4, 0xFFFFFFFF);
            UiDraw.diamond(g, qx, qy + bob, 3, 0xFFF5C95A);
        }
        // игрок: расходящиеся круги и стрелка
        int cx = x1 + SIZE / 2;
        int cy = y1 + SIZE / 2;
        double ph = (time % 1800L) / 1800.0D;
        int rr = 5 + (int) (ph * 11.0D);
        int ra = (int) ((1.0D - ph) * 150.0D);
        for (int n = 0; n < 16; n++) {
            double ang = n * Math.PI / 8.0D;
            int rx = cx + (int) Math.round(Math.cos(ang) * rr);
            int ry = cy + (int) Math.round(Math.sin(ang) * rr);
            g.fill(rx, ry, rx + 1, ry + 1, (ra << 24) | 0xF08AB0);
        }
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(player.getYRot() + 180.0F));
        for (int i = 0; i < 8; i++) {
            int hw = i / 2 + 1;
            g.fill(-hw - 1, -6 + i, hw + 2, -5 + i, 0xFFFFFFFF);
        }
        for (int i = 0; i < 7; i++) {
            int hw = i / 2;
            g.fill(-hw, -5 + i, hw + 1, -4 + i, 0xFFF08AB0);
        }
        g.pose().popPose();
        g.disableScissor();

        // рамка в цвете настроек, север и координаты
        int c = 0xFF000000 | MapSettings.frame;
        g.fill(x1 - 2, y1 - 2, x2 + 2, y1, c);
        g.fill(x1 - 2, y2, x2 + 2, y2 + 2, c);
        g.fill(x1 - 2, y1, x1, y2, c);
        g.fill(x2, y1, x2 + 2, y2, c);
        UiDraw.circle(g, x1 - 1, y1 - 1, 2, c);
        UiDraw.circle(g, x2 + 1, y1 - 1, 2, c);
        UiDraw.circle(g, x1 - 1, y2 + 1, 2, c);
        UiDraw.circle(g, x2 + 1, y2 + 1, 2, c);
        UiDraw.rounded(g, cx - 5, y1 - 7, cx + 5, y1 + 4, 4, c);
        g.drawString(mc.font, "N", cx - 2, y1 - 5, 0xFFFFFFFF, false);
        String coords = (int) px + "  " + (int) pz;
        int tw = mc.font.width(coords);
        UiDraw.rounded(g, x1 + SIZE / 2 - tw / 2 - 4, y2 + 3, x1 + SIZE / 2 + tw / 2 + 4, y2 + 14, 5, 0xB8FFFFFF);
        g.drawString(mc.font, coords, x1 + SIZE / 2 - tw / 2, y2 + 5, 0xFF7A6A80, false);
    }
}
