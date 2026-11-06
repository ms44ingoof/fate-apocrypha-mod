package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.entity.StoryCameraEntity;
import com.example.fateapocrypha.entity.StoryNpcEntity;
import com.example.fateapocrypha.network.CameraPacket;
import com.example.fateapocrypha.network.ModNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Состояние одной запущенной сцены. */
public class SceneContext {
    public final ServerPlayer player;
    public final Map<String, UUID> npcs = new HashMap<>();
    public final List<UUID> mobs = new ArrayList<>();
    public UUID cameraId;
    public boolean cinematic;
    public int stepTicks;
    public int skipTick = -1000;

    public SceneContext(ServerPlayer player) {
        this.player = player;
    }

    /** Игрок нажал пропуск недавно: реплика заканчивается. Запрос сгорает через несколько тиков. */
    public boolean consumeSkip() {
        if (player.tickCount - skipTick <= 4) {
            skipTick = -1000;
            return true;
        }
        return false;
    }

    public ServerLevel trifas() {
        return player.getServer() == null ? null : player.getServer().getLevel(ModDimensions.TRIFAS);
    }

    public StoryNpcEntity npc(String key) {
        UUID id = npcs.get(key);
        ServerLevel level = trifas();
        if (id == null || level == null) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof StoryNpcEntity npc ? npc : null;
    }

    public StoryCameraEntity camera() {
        ServerLevel level = trifas();
        if (cameraId == null || level == null) {
            return null;
        }
        Entity entity = level.getEntity(cameraId);
        return entity instanceof StoryCameraEntity camera ? camera : null;
    }

    /** Заканчивает кино-режим: убирает камеру, снимает невидимость и неуязвимость, возвращает вид игроку. */
    public void endCinematic() {
        if (!cinematic) {
            return;
        }
        cinematic = false;
        StoryCameraEntity camera = camera();
        if (camera != null) {
            camera.discard();
        }
        cameraId = null;
        player.setInvulnerable(false);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.JUMP);
        try {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CameraPacket(-1, false));
        } catch (RuntimeException ignored) {
            // игрок уже отключается
        }
    }
}
