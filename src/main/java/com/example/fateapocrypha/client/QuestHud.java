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
        int x = 8;
        int y = 8;
        int pw = 178;
        List<FormattedCharSequence> lines = font.split(Component.translatable(chapter.objectiveKey()), pw - 16);
        int ph = 30 + lines.size() * 10;
        g.fill(x, y, x + pw, y + ph, 0xB0141824);
        g.fill(x, y, x + 2, y + ph, GOLD);
        g.drawString(font, Component.translatable("hud.fateapocrypha.quest"), x + 8, y + 4, 0xFFB8A878, false);
        g.drawString(font, Component.translatable(chapter.titleKey()), x + 8, y + 15, 0xFFFFFFFF, true);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x + 8, y + 27 + i * 10, 0xFFCFD3DC, false);
        }
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
            // цель за краем экрана: метка прижимается к краю в сторону цели
            double dx = xc;
            double dy = -yc;
            if (Math.hypot(dx, dy) < 1.0e-3D) {
                dx = 0.0D;
                dy = 1.0D;
            }
            double scaleX = Math.abs(dx) < 1.0e-6D ? 1.0e9D : (w / 2.0D - 46.0D) / Math.abs(dx);
            double scaleY = Math.abs(dy) < 1.0e-6D ? 1.0e9D : (h / 2.0D - 56.0D) / Math.abs(dy);
            double scale = Math.min(scaleX, scaleY);
            sx = w / 2.0D + dx * scale;
            sy = h / 2.0D + dy * scale;
        }

        double dist = Math.max(0.0D, Math.hypot(mc.player.getX() - tx, mc.player.getZ() - tz) - location.radius());
        float time = (mc.level.getGameTime() + partial) * 0.15F;
        float pulse = 1.0F + 0.12F * (float) Math.sin(time);
        Font font = mc.font;

        g.pose().pushPose();
        g.pose().translate((float) sx, (float) sy, 0.0F);
        g.pose().scale(pulse, pulse, 1.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45.0F));
        g.fill(-7, -7, 7, 7, 0xFF141824);
        g.fill(-6, -6, 6, 6, GOLD);
        g.fill(-4, -4, 4, 4, 0xFFFFF1B8);
        g.pose().popPose();

        g.drawCenteredString(font, Component.translatable(location.nameKey()), (int) sx, (int) sy - 26, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("hud.fateapocrypha.meters", Math.round(dist)),
                (int) sx, (int) sy + 12, GOLD);
    }
}
