package com.example.fateapocrypha.client;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.world.TrifasTerrain;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

/** Панель текущего задания и метка цели на экране с расстоянием в метрах. */
public class QuestHud {
    private static final int GOLD = 0xFFE6C25A;

    public static void render(GuiGraphics g, int w, int h, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || ClientStoryState.isCinematic()) {
            return;
        }
        StoryChapter chapter = ClientQuestState.chapter();
        if (chapter == StoryChapter.TO_BE_CONTINUED) {
            return;
        }
        drawTracker(g, mc, chapter);
        StoryLocation location = StoryLocation.forChapter(chapter);
        if (location != null && !ClientQuestState.started() && mc.level.dimension().equals(ModDimensions.TRIFAS)) {
            drawMarker(g, mc, location, w, h, partial);
        }
    }

    private static void drawTracker(GuiGraphics g, Minecraft mc, StoryChapter chapter) {
        Font font = mc.font;
        int x = 10;
        int y = 10;
        int pw = 184;
        List<FormattedCharSequence> lines = font.split(Component.translatable(chapter.objectiveKey()), pw - 24);
        int ph = 36 + lines.size() * 10;
        UiDraw.shadow(g, x, y, x + pw, y + ph, 7);
        UiDraw.outlined(g, x, y, x + pw, y + ph, 7, 0x88E6C25A, 0xD8222A3E, 0xD8141926);
        UiDraw.rounded(g, x + 5, y + 8, x + 8, y + ph - 8, 1, GOLD);
        UiDraw.diamond(g, x + 19, y + 11, 3, GOLD);
        g.drawString(font, Component.translatable("hud.fateapocrypha.quest"), x + 27, y + 7, 0xFFB8A878, false);
        g.drawString(font, Component.translatable(chapter.titleKey()), x + 14, y + 19, 0xFFFFFFFF, true);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + 14, y + 31 + i * 10, 0xFFCFD3DC, false);
        }
    }

    private static void pill(GuiGraphics g, Font font, Component text, int cx, int y, int color) {
        int tw = font.width(text);
        int x1 = cx - tw / 2 - 7;
        int x2 = cx + tw / 2 + 7;
        UiDraw.outlined(g, x1, y, x2, y + 14, 7, 0x99E6C25A, 0xE0181D2C, 0xE00F1320);
        g.drawString(font, text, cx - tw / 2, y + 3, color, false);
    }

    private static void drawMarker(GuiGraphics g, Minecraft mc, StoryLocation location, int w, int h, float partial) {
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cp = camera.getPosition();
        double tx = location.cx() + 0.5D;
        double tz = location.cz() + 0.5D;
        double ty = TrifasTerrain.height(location.cx(), location.cz()) + (location == StoryLocation.GARDENS ? 70.0D : 26.0D);

        double yaw = Math.toRadians(camera.getYRot());
        double pitch = Math.toRadians(camera.getXRot());
        Vec3 fwd = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        Vec3 up = right.cross(fwd);
        Vec3 v = new Vec3(tx - cp.x, ty - cp.y, tz - cp.z);
        double zc = v.dot(fwd);
        double xc = v.dot(right);
        double yc = v.dot(up);
        double tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0D);
        double aspect = (double) w / (double) h;

        double sx = w / 2.0D;
        double sy = h / 2.0D;
        boolean onScreen = false;
        if (zc > 0.05D) {
            double nx = xc / (zc * tanHalf * aspect);
            double ny = yc / (zc * tanHalf);
            if (Math.abs(nx) <= 0.9D && Math.abs(ny) <= 0.78D) {
                sx = w / 2.0D + nx * w / 2.0D;
                sy = h / 2.0D - ny * h / 2.0D;
                onScreen = true;
            }
        }
        if (!onScreen) {
            double dx = xc;
            double dy = -yc;
            if (Math.hypot(dx, dy) < 1.0e-3D) {
                dx = 0.0D;
                dy = 1.0D;
            }
            double scaleX = Math.abs(dx) < 1.0e-6D ? 1.0e9D : (w / 2.0D - 56.0D) / Math.abs(dx);
            double scaleY = Math.abs(dy) < 1.0e-6D ? 1.0e9D : (h / 2.0D - 66.0D) / Math.abs(dy);
            double scale = Math.min(scaleX, scaleY);
            sx = w / 2.0D + dx * scale;
            sy = h / 2.0D + dy * scale;
        }

        double dist = Math.max(0.0D, Math.hypot(mc.player.getX() - tx, mc.player.getZ() - tz) - location.radius());
        float time = (mc.level.getGameTime() + partial) * 0.15F;
        float pulse = 1.0F + 0.1F * (float) Math.sin(time);
        Font font = mc.font;
        int ix = (int) sx;
        int iy = (int) sy;

        // мягкое свечение, ромб-метка
        UiDraw.circle(g, ix, iy, (int) (15 * pulse), 0x22E6C25A);
        UiDraw.circle(g, ix, iy, (int) (11 * pulse), 0x33E6C25A);
        g.pose().pushPose();
        g.pose().translate((float) sx, (float) sy, 0.0F);
        g.pose().scale(pulse, pulse, 1.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
        UiDraw.rounded(g, -8, -8, 8, 8, 3, 0xFF141824);
        UiDraw.gradient(g, -7, -7, 7, 7, 2, 0xFFFFE08A, 0xFFD9A93A);
        UiDraw.rounded(g, -3, -3, 3, 3, 1, 0xFFFFF6D6);
        g.pose().popPose();

        pill(g, font, Component.translatable(location.nameKey()), ix, iy - 32, 0xFFFFFFFF);
        pill(g, font, Component.translatable("hud.fateapocrypha.meters", Math.round(dist)), ix, iy + 14, GOLD);
    }
}
