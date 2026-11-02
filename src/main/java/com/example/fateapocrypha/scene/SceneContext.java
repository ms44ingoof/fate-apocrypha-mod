package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.entity.StoryNpcEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Состояние одной запущенной сцены. */
public class SceneContext {
    public final ServerPlayer player;
    public final Map<String, UUID> npcs = new HashMap<>();
    public final List<UUID> mobs = new ArrayList<>();
    public int stepTicks;

    public SceneContext(ServerPlayer player) {
        this.player = player;
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
}
