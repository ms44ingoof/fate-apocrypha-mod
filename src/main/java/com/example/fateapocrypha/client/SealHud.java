package com.example.fateapocrypha.client;

import com.example.fateapocrypha.entity.ServantEntity;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.SealActionPacket;
import com.example.fateapocrypha.servant.ServantActions;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Печать Командных заклинаний на экране: три знака с клавишами (Z, X, C по умолчанию, переназначаются).
 * Приказ применяется удержанием клавиши около полсекунды (защита от случайных нажатий) и уходит на перезарядку:
 * заряды не тратятся, у каждого приказа своя перезарядка.
 */
public class SealHud {
    /** Сколько тиков держать клавишу. Поставьте 0, чтобы приказы применялись мгновенно. */
    public static final int HOLD_TICKS = 8;

    private static final int RED = 0xFFD92E3A;
    private static final int RED_DARK = 0xFF5B1C24;
    private static final int GOLD = 0xFFE6C25A;

    private static final KeyMapping[] KEYS = {ClientKeys.SEAL_TELEPORT, ClientKeys.SEAL_BUFF, ClientKeys.SEAL_PHANTASM};
    private static final int[] ACTIONS = {ServantActions.ACTION_TELEPORT, ServantActions.ACTION_BUFF, ServantActions.ACTION_PHANTASM};
    private static final int[] COOLDOWNS = {ServantEntity.TELEPORT_COOLDOWN_TICKS, ServantEntity.BUFF_COOLDOWN_TICKS,
            ServantEntity.PHANTASM_COOLDOWN_TICKS};
    private static final String[] NAMES = {"hud.fateapocrypha.seal.teleport", "hud.fateapocrypha.seal.buff", "hud.fateapocrypha.seal.phantasm"};

    private static final int[] hold = new int[3];
    private static final boolean[] fired = new boolean[3];
    private static final int[] remaining = new int[3];
    private static boolean servantFound = false;
    private static ServantEntity cached;

    private static boolean active(Minecraft mc) {
        return mc.player != null && mc.level != null && ClientQuestState.hasSeals();
    }

    private static ServantEntity findServant(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        List<ServantEntity> servants = mc.level.getEntitiesOfClass(ServantEntity.class,
                mc.player.getBoundingBox().inflate(256.0D), s -> s.isOwnedBy(mc.player) && s.isAlive());
        return servants.isEmpty() ? null : servants.get(0);
    }

    /** Раз в тик: обновляет перезарядки и считает удержание клавиш. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc)) {
            java.util.Arrays.fill(hold, 0);
            return;
        }
        if (cached == null || !cached.isAlive() || mc.player.tickCount % 20 == 0) {
            cached = findServant(mc);
        }
        servantFound = cached != null && cached.isAlive();
        remaining[0] = servantFound ? cached.getTeleportRemaining() : 0;
        remaining[1] = servantFound ? cached.getBuffRemaining() : 0;
        remaining[2] = servantFound ? cached.getPhantasmRemaining() : 0;

        boolean blocked = mc.screen != null || ClientStoryState.isCinematic();
        for (int i = 0; i < 3; i++) {
            if (blocked || !KEYS[i].isDown()) {
                hold[i] = 0;
                fired[i] = false;
                continue;
            }
            if (fired[i]) {
                continue;
            }
            hold[i]++;
            if (hold[i] >= HOLD_TICKS) {
                fired[i] = true;
                hold[i] = HOLD_TICKS;
                if (remaining[i] <= 0 || !servantFound) {
                    ModNetwork.CHANNEL.sendToServer(new SealActionPacket(ACTIONS[i]));
                }
            }
        }
    }

    public static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc) || mc.options.hideGui || ClientStoryState.isCinematic()) {
            return;
        }
        int size = 28;
        int gap = 8;
        int total = size * 3 + gap * 2;
        int x0 = w - total - 12;
        int y = h - 64;
        for (int i = 0; i < 3; i++) {
            int x = x0 + i * (size + gap);
            boolean ready = servantFound && remaining[i] <= 0;
            int color = ready ? RED : RED_DARK;

            g.fill(x - 1, y - 1, x + size + 1, y + size + 1, ready ? GOLD : 0xFF3A2A2E);
            g.fill(x, y, x + size, y + size, 0xE0120C10);
            glyph(g, i, x + size / 2, y + size / 2, color, ready);

            if (servantFound && remaining[i] > 0) {
                // перезарядка: тёмная заливка сверху вниз и секунды
                int cover = (int) Math.ceil((double) size * remaining[i] / COOLDOWNS[i]);
                g.fill(x, y, x + size, y + cover, 0xA0000000);
                String sec = String.valueOf((remaining[i] + 19) / 20);
                g.drawCenteredString(mc.font, sec, x + size / 2, y + size / 2 - 4, 0xFFFFFFFF);
            }

            Component key = KEYS[i].getTranslatedKeyMessage();
            g.drawCenteredString(mc.font, key, x + size / 2, y + size + 3, ready ? 0xFFFFFFFF : 0xFF8A7F83);

            // удержание: полоска под знаком и название приказа над ним
            if (hold[i] > 0 && HOLD_TICKS > 0) {
                int fill = size * hold[i] / HOLD_TICKS;
                g.fill(x, y + size - 2, x + fill, y + size, GOLD);
                g.drawCenteredString(mc.font, Component.translatable(NAMES[i]), x + size / 2, y - 12, GOLD);
            }
        }
    }

    /** Знак приказа из заливок: телепорт (портал), усиление (крест), Фантазм (звезда). */
    private static void glyph(GuiGraphics g, int kind, int cx, int cy, int color, boolean ready) {
        int core = ready ? GOLD : 0xFF7A5A5E;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        if (kind == 0) {
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0F));
            g.fill(-8, -8, 8, 8, color);
            g.fill(-6, -6, 6, 6, 0xFF120C10);
            g.fill(-4, -4, 4, 4, color);
            g.fill(-2, -2, 2, 2, core);
        } else if (kind == 1) {
            g.fill(-2, -9, 2, 9, color);
            g.fill(-9, -2, 9, 2, color);
            g.fill(-1, -5, 1, 5, core);
            g.fill(-5, -1, 5, 1, core);
        } else {
            g.fill(-2, -10, 2, 10, color);
            g.fill(-10, -2, 10, 2, color);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0F));
            g.fill(-2, -8, 2, 8, color);
            g.fill(-8, -2, 8, 2, color);
            g.fill(-3, -3, 3, 3, core);
        }
        g.pose().popPose();
    }
}
