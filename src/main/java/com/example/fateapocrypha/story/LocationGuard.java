package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.scene.SceneManager;
import com.example.fateapocrypha.scene.StoryWorld;
import com.example.fateapocrypha.world.WorldDecor;
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
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Физические границы сюжетных локаций, вход в нужную локацию (запуск главы) и подсказка направления.
 * Вызывается каждый тик игрока из StoryEvents.
 */
public class LocationGuard {
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
        // мир живёт вокруг игрока: локации строятся заранее (ворота видны издалека), появляются дома и колодцы
        if (player.tickCount % 100 == 0) {
            prebuild(player);
        }
        if (player.tickCount % 40 == 0) {
            WorldDecor.tick(player.serverLevel(), player);
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
    }

    /** Строит локации, до которых игроку осталось меньше 280 блоков (с запасом, чтобы не было задержки у входа). */
    private static void prebuild(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (StoryLocation location : StoryLocation.values()) {
            if (location.distanceTo(player.getX(), player.getZ()) > location.radius() + 280.0D) {
                continue;
            }
            switch (location) {
                case CASTLE_LAB, CASTLE_CORRIDOR, CASTLE_GATE -> StoryWorld.ensureBuilt(level);
                case FOREST -> StoryWorld.ensureForest(level);
                case RED_CAMP, CHAPEL, RUINS -> StoryWorld.ensureStages(level);
                case STREET, SIEGE, LONDON, PLAIN -> StoryWorld.ensureStagesB(level);
                case FARM -> StoryWorld.ensureFarm(level);
                case GARDENS -> StoryWorld.ensureGardens(level);
                default -> {
                }
            }
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
}
