package com.example.fateapocrypha.scene;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Хранит запущенные сцены игроков. */
public class SceneManager {
    private static final Map<UUID, SceneRunner> RUNNERS = new HashMap<>();
    private static final Set<UUID> MANAGED_NPCS = new HashSet<>();

    public static void start(ServerPlayer player, List<Step> steps) {
        stop(player.getUUID());
        RUNNERS.put(player.getUUID(), new SceneRunner(player, steps));
    }

    public static boolean isRunning(UUID playerId) {
        return RUNNERS.containsKey(playerId);
    }

    public static void stop(UUID playerId) {
        SceneRunner runner = RUNNERS.remove(playerId);
        if (runner != null) {
            runner.cancel();
        }
    }

    public static void tick(ServerPlayer player) {
        SceneRunner runner = RUNNERS.get(player.getUUID());
        if (runner != null && runner.tick()) {
            RUNNERS.remove(player.getUUID(), runner);
        }
    }

    /** Игрок просит пропустить реплику. */
    public static void requestSkip(ServerPlayer player) {
        SceneRunner runner = RUNNERS.get(player.getUUID());
        if (runner != null) {
            runner.context().skipTick = player.tickCount;
        }
    }

    public static void clear() {
        for (SceneRunner runner : new HashSet<>(RUNNERS.values())) {
            runner.cancel();
        }
        RUNNERS.clear();
        MANAGED_NPCS.clear();
    }

    public static void register(UUID npcId) {
        MANAGED_NPCS.add(npcId);
    }

    public static void unregister(UUID npcId) {
        MANAGED_NPCS.remove(npcId);
    }

    public static boolean isManaged(UUID npcId) {
        return MANAGED_NPCS.contains(npcId);
    }
}
