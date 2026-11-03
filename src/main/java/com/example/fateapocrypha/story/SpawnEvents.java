package com.example.fateapocrypha.story;

import com.example.fateapocrypha.FateApocrypha;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Враждебные мобы не появляются сами (на любой сложности). Мобы сюжетных сцен создаются кодом и не затронуты. */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public class SpawnEvents {

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION
                || type == MobSpawnType.REINFORCEMENT || type == MobSpawnType.PATROL
                || type == MobSpawnType.JOCKEY || type == MobSpawnType.EVENT) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Фантомы появляются отдельным механизмом, поэтому отключаем их правилом игры. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        event.getServer().getGameRules().getRule(GameRules.RULE_DOINSOMNIA).set(false, event.getServer());
    }
}
