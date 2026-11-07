package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.scene.SceneManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Физические границы сюжетных локаций, вход в нужную локацию (запуск главы) и подсказка направления.
 * Вызывается каждый тик игрока из StoryEvents.
 */
public class LocationGuard {
    private static final String[] ARROWS = {"\u2B06", "\u2B08", "\u27A1", "\u2B0A", "\u2B07", "\u2B0B", "\u2B05", "\u2B09"};

    /** true: игрок сейчас внутри закрытой локации (ему разрешено выйти, но не заходить глубже заново). */
    private static final Map<UUID, Boolean> INSIDE = new HashMap<>();

    public static void forget(UUID id) {
        INSIDE.remove(id);
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 5 != 0) {
            return;
        }
        if (!player.level().dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        StoryChapter chapter = StoryManager.getChapter(player);
        if (chapter.ordinal() >= StoryChapter.TO_BE_CONTINUED.ordinal()) {
            return;   // сюжет пройден: мир открыт
        }
        UUID id = player.getUUID();
        StoryLocation open = StoryLocation.forChapter(chapter);
        StoryLocation in = StoryLocation.at(player.getX(), player.getZ());

        if (SceneManager.isRunning(id)) {
            INSIDE.put(id, in != null);
            return;
        }

        // вход в нужную локацию запускает сцену главы
        if (open != null && in == open && !StoryManager.isStarted(player)) {
            INSIDE.put(id, false);
            StoryDirector.startScene(player, true);
            return;
        }

        // закрытая локация: нельзя заходить, но можно выйти, если игрок уже внутри
        if (in != null && in != open) {
            if (!INSIDE.getOrDefault(id, true)) {
                pushOut(player, in);
                return;
            }
            INSIDE.put(id, true);
            return;
        }
        INSIDE.put(id, false);

        if (player.tickCount % 10 == 0) {
            drawBorder(player, open);
        }
        if (open != null && !StoryManager.isStarted(player) && player.tickCount % 40 == 0) {
            guide(player, open);
        }
    }

    private static void pushOut(ServerPlayer player, StoryLocation zone) {
        double dx = player.getX() - zone.cx();
        double dz = player.getZ() - zone.cz();
        double len = Math.hypot(dx, dz);
        if (len < 0.001D) {
            dx = 1.0D;
            dz = 0.0D;
            len = 1.0D;
        }
        double tx = zone.cx() + dx / len * (zone.radius() + 4);
        double tz = zone.cz() + dz / len * (zone.radius() + 4);
        ServerLevel level = player.serverLevel();
        int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) tx, (int) tz);
        player.stopRiding();
        player.teleportTo(level, tx + 0.5D, ty, tz + 0.5D, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.translatable("story.fateapocrypha.location_closed",
                Component.translatable(zone.nameKey())).withStyle(ChatFormatting.RED), true);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    /** Мерцающая стена на границе ближайшей закрытой локации. */
    private static void drawBorder(ServerPlayer player, StoryLocation open) {
        ServerLevel level = player.serverLevel();
        for (StoryLocation zone : StoryLocation.values()) {
            if (zone == open) {
                continue;
            }
            double d = zone.distanceTo(player.getX(), player.getZ()) - zone.radius();
            if (d <= 0.0D || d > 14.0D) {
                continue;
            }
            double base = Math.atan2(player.getZ() - zone.cz(), player.getX() - zone.cx());
            for (int k = -8; k <= 8; k++) {
                double a = base + k * 0.05D;
                double x = zone.cx() + zone.radius() * Math.cos(a);
                double z = zone.cz() + zone.radius() * Math.sin(a);
                for (int h = 0; h < 4; h++) {
                    level.sendParticles(ParticleTypes.END_ROD, x, player.getY() + h * 1.4D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    /** Подсказка в панели действий: стрелка и расстояние до цели. */
    private static void guide(ServerPlayer player, StoryLocation target) {
        double dx = target.cx() - player.getX();
        double dz = target.cz() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = Mth.wrapDegrees(angle - player.getYRot());
        int index = Math.floorMod(Math.round((float) (rel / 45.0D)), 8);
        long distance = Math.round(Math.hypot(dx, dz));
        player.displayClientMessage(Component.translatable("story.fateapocrypha.go_to", ARROWS[index],
                Component.translatable(target.nameKey()), distance).withStyle(ChatFormatting.GOLD), true);
    }
}
