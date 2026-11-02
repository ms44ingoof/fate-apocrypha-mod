package com.example.fateapocrypha.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** Клиентская часть катсцен: рамка диалога и затемнение экрана. */
public class ClientStoryState {
    private static Component speaker = Component.empty();
    private static Component text = Component.empty();
    private static long dialogueEnd = 0L;

    private static long fadeStart = 0L;
    private static long fadeIn = 0L;
    private static long fadeHold = 0L;
    private static long fadeOut = 0L;

    private static boolean cinematic = false;
    private static int pendingCamera = -2;

    public static boolean isCinematic() {
        return cinematic;
    }

    /** entityId >= 0: смотреть глазами этой сущности; -1: вернуть камеру игроку. */
    public static void setCamera(int entityId, boolean on) {
        cinematic = on;
        pendingCamera = entityId;
    }

    /** Каждый клиентский тик: ждём появления сущности-камеры и переключаем на неё вид. */
    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cinematic = false;
            pendingCamera = -2;
            return;
        }
        if (pendingCamera == -1) {
            mc.setCameraEntity(mc.player);
            pendingCamera = -2;
        } else if (pendingCamera >= 0) {
            Entity camera = mc.level.getEntity(pendingCamera);
            if (camera != null) {
                mc.setCameraEntity(camera);
                pendingCamera = -2;
            }
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    public static void showDialogue(Component newSpeaker, Component newText, int ticks) {
        speaker = newSpeaker;
        text = newText;
        dialogueEnd = now() + ticks * 50L;
    }

    public static void startFade(int in, int hold, int out) {
        fadeStart = now();
        fadeIn = in * 50L;
        fadeHold = hold * 50L;
        fadeOut = out * 50L;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        long t = now();
        renderFade(graphics, width, height, t);
        if (cinematic) {
            int bar = height / 8;
            graphics.fill(0, 0, width, bar, 0xFF000000);
            graphics.fill(0, height - bar, width, height, 0xFF000000);
        }
        renderDialogue(graphics, width, height, t);
    }

    private static void renderFade(GuiGraphics graphics, int width, int height, long t) {
        if (fadeStart == 0L) {
            return;
        }
        long elapsed = t - fadeStart;
        if (elapsed >= fadeIn + fadeHold + fadeOut) {
            return;
        }
        float alpha;
        if (elapsed < fadeIn) {
            alpha = fadeIn == 0L ? 1.0F : elapsed / (float) fadeIn;
        } else if (elapsed < fadeIn + fadeHold) {
            alpha = 1.0F;
        } else {
            alpha = 1.0F - (elapsed - fadeIn - fadeHold) / (float) fadeOut;
        }
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        graphics.fill(0, 0, width, height, a << 24);
    }

    private static void renderDialogue(GuiGraphics graphics, int width, int height, long t) {
        if (t >= dialogueEnd) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int boxWidth = Math.min(width - 40, 420);
        List<FormattedCharSequence> lines = font.split(text, boxWidth - 20);
        int boxHeight = 24 + lines.size() * 10;
        int x = (width - boxWidth) / 2;
        int bottom = cinematic ? height - height / 8 - 12 : height - 70;
        int y = bottom - boxHeight;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xC0000000);
        graphics.fill(x, y, x + boxWidth, y + 1, 0xFFD4AF37);
        graphics.drawString(font, speaker, x + 10, y + 6, 0xFFD700, true);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + 10, y + 20 + i * 10, 0xFFFFFF, true);
        }
    }
}
