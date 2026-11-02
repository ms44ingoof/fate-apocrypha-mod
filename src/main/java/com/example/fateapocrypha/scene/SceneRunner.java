package com.example.fateapocrypha.scene;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Выполняет шаги сцены по порядку. */
public class SceneRunner {
    private final List<Step> steps;
    private final SceneContext ctx;
    private int index = 0;
    private boolean began = false;
    private boolean cancelled = false;

    public SceneRunner(ServerPlayer player, List<Step> steps) {
        this.steps = steps;
        this.ctx = new SceneContext(player);
    }

    /** @return true, когда сцена закончилась */
    public boolean tick() {
        while (!cancelled && index < steps.size()) {
            Step step = steps.get(index);
            if (!began) {
                ctx.stepTicks = 0;
                step.begin(ctx);
                began = true;
                if (cancelled) {
                    break;
                }
            }
            if (step.update(ctx)) {
                index++;
                began = false;
            } else {
                ctx.stepTicks++;
                break;
            }
        }
        return cancelled || index >= steps.size();
    }

    /** Останавливает сцену и убирает её NPC и мобов. */
    public void cancel() {
        cancelled = true;
        ServerLevel level = ctx.trifas();
        for (UUID id : ctx.npcs.values()) {
            SceneManager.unregister(id);
            discard(level, id);
        }
        for (UUID id : ctx.mobs) {
            discard(level, id);
        }
        ctx.npcs.clear();
        ctx.mobs.clear();
    }

    private static void discard(ServerLevel level, UUID id) {
        if (level != null) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }
}
