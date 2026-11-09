package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.entity.ServantEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.SealActionPacket;
import com.example.fateapocrypha.servant.ServantActions;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Печать Командных заклинаний на экране: три знака с клавишами (Z, X, C по умолчанию, переназначаются).
 * Приказ применяется удержанием клавиши около полсекунды (защита от случайных нажатий) и уходит на перезарядку:
 * заряды не тратятся, у каждого приказа своя перезарядка.
 */
public class SealHud {
    /** Сколько тиков держать клавишу. Поставьте 0, чтобы приказы применялись мгновенно. */
    public static final int HOLD_TICKS = 8;

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

    private static final ResourceLocation EMBLEM = new ResourceLocation(FateApocrypha.MODID, "textures/gui/seal_emblem.png");
    private static final ResourceLocation[] ICONS = {
            new ResourceLocation(FateApocrypha.MODID, "textures/gui/seal_teleport.png"),
            new ResourceLocation(FateApocrypha.MODID, "textures/gui/seal_buff.png"),
            new ResourceLocation(FateApocrypha.MODID, "textures/gui/seal_phantasm.png")};
    private static final int EMBLEM_SIZE = 96;
    private static final int ICON_SIZE = 32;
    private static final int RING_RADIUS = 66;

    /** Эмблема печати в правом нижнем углу и три приказа дугой вокруг неё (Z слева, X по диагонали, C сверху). */
    public static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!active(mc) || mc.options.hideGui || ClientStoryState.isCinematic()) {
            return;
        }
        int ex = w - 12 - EMBLEM_SIZE;
        int ey = h - 12 - EMBLEM_SIZE;
        int cx = ex + EMBLEM_SIZE / 2;
        int cy = ey + EMBLEM_SIZE / 2;

        boolean anyReady = false;
        boolean holding = false;
        for (int i = 0; i < 3; i++) {
            anyReady |= servantFound && remaining[i] <= 0;
            holding |= hold[i] > 0;
        }
        float time = (mc.level.getGameTime() + mc.getFrameTime()) * 0.12F;
        float pulse = anyReady ? 0.82F + 0.18F * (float) Math.sin(time) : 0.45F;
        if (holding) {
            pulse = 1.0F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // тонкая дуга-направляющая для приказов
        for (int a = 168; a <= 282; a += 3) {
            double rad = Math.toRadians(a);
            int dx = cx + (int) Math.round(RING_RADIUS * Math.cos(rad));
            int dy = cy + (int) Math.round(RING_RADIUS * Math.sin(rad));
            g.fill(dx, dy, dx + 1, dy + 1, 0x66D92E3A);
        }

        // эмблема печати
        g.setColor(pulse, pulse * 0.92F, pulse * 0.92F, 1.0F);
        g.blit(EMBLEM, ex, ey, 0.0F, 0.0F, EMBLEM_SIZE, EMBLEM_SIZE, EMBLEM_SIZE, EMBLEM_SIZE);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < 3; i++) {
            double ang = Math.toRadians(180 + 45 * i);
            int mx = cx + (int) Math.round(RING_RADIUS * Math.cos(ang)) - ICON_SIZE / 2;
            int my = cy + (int) Math.round(RING_RADIUS * Math.sin(ang)) - ICON_SIZE / 2;
            boolean ready = servantFound && remaining[i] <= 0;

            float tint = ready ? 1.0F : 0.42F;
            g.setColor(tint, tint, tint, 1.0F);
            g.blit(ICONS[i], mx, my, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (servantFound && remaining[i] > 0) {
                // перезарядка: тёмная заливка сверху вниз в границах круглого медальона
                int cover = (int) Math.ceil((double) ICON_SIZE * remaining[i] / COOLDOWNS[i]);
                for (int row = 0; row < Math.min(cover, ICON_SIZE); row++) {
                    double dyr = row + 0.5D - ICON_SIZE / 2.0D;
                    double half = Math.sqrt(Math.max(0.0D, 15.5D * 15.5D - dyr * dyr));
                    int hw = (int) half;
                    if (hw > 0) {
                        g.fill(mx + ICON_SIZE / 2 - hw, my + row, mx + ICON_SIZE / 2 + hw, my + row + 1, 0xB0000000);
                    }
                }
                g.drawCenteredString(mc.font, String.valueOf((remaining[i] + 19) / 20),
                        mx + ICON_SIZE / 2, my + ICON_SIZE / 2 - 4, 0xFFFFFFFF);
            }

            // клавиша под медальоном
            Component key = KEYS[i].getTranslatedKeyMessage();
            g.drawCenteredString(mc.font, key, mx + ICON_SIZE / 2, my + ICON_SIZE + 2, ready ? 0xFFFFFFFF : 0xFF8A7F83);

            // удержание: полоска под медальоном и название приказа над ним
            if (hold[i] > 0 && HOLD_TICKS > 0) {
                int fill = ICON_SIZE * hold[i] / HOLD_TICKS;
                g.fill(mx, my + ICON_SIZE - 1, mx + fill, my + ICON_SIZE + 1, GOLD);
                g.drawCenteredString(mc.font, Component.translatable(NAMES[i]), mx + ICON_SIZE / 2, my - 12, GOLD);
            }
        }
        RenderSystem.disableBlend();
    }
}
