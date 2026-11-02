package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModEntities;
import com.example.fateapocrypha.entity.StoryNpcEntity;
import com.example.fateapocrypha.network.DialoguePacket;
import com.example.fateapocrypha.network.FadePacket;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.servant.ServantActions;
import com.example.fateapocrypha.story.NpcType;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryManager;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** Набор готовых шагов для сцен. */
public class Steps {

    public static Step wait(int ticks) {
        return new Step() {
            @Override
            public boolean update(SceneContext c) {
                return c.stepTicks >= ticks;
            }
        };
    }

    /** Реплика: рамка внизу экрана + копия в чате. */
    public static Step say(String speakerKey, String textKey, int ticks) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                Component speaker = Component.translatable(speakerKey);
                Component text = Component.translatable(textKey);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> c.player),
                        new DialoguePacket(speaker, text, ticks));
                c.player.sendSystemMessage(Component.empty()
                        .append(speaker.copy().withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(": "))
                        .append(text));
            }

            @Override
            public boolean update(SceneContext c) {
                return c.stepTicks >= ticks;
            }
        };
    }

    /** Затемнение экрана. wait = true: сцена ждёт, пока экран станет чёрным и пройдёт hold. */
    public static Step fade(int in, int hold, int out, boolean wait) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> c.player),
                        new FadePacket(in, hold, out));
            }

            @Override
            public boolean update(SceneContext c) {
                return !wait || c.stepTicks >= in + hold;
            }
        };
    }

    public static Step title(String titleKey, String subtitleKey) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                c.player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                c.player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(subtitleKey)));
                c.player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(titleKey)));
            }

            @Override
            public boolean update(SceneContext c) {
                return true;
            }
        };
    }

    public static Step run(Consumer<SceneContext> action) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                action.accept(c);
            }

            @Override
            public boolean update(SceneContext c) {
                return true;
            }
        };
    }

    public static Step teleport(double x, double y, double z, float yaw) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level != null) {
                c.player.teleportTo(level, x, y, z, yaw, 0.0F);
                c.player.setDeltaMovement(Vec3.ZERO);
                c.player.fallDistance = 0.0F;
            }
        });
    }

    public static Step effect(MobEffect effect, int duration, int amplifier) {
        return run(c -> c.player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false)));
    }

    public static Step clearEffects() {
        return run(c -> c.player.removeAllEffects());
    }

    public static Step sound(SoundEvent sound) {
        return run(c -> c.player.level().playSound(null, c.player.blockPosition(), sound,
                SoundSource.PLAYERS, 1.0F, 1.0F));
    }

    public static Step give(Supplier<ItemStack> stack) {
        return run(c -> ServantActions.give(c.player, stack.get()));
    }

    public static Step waitUntil(Predicate<SceneContext> condition) {
        return new Step() {
            @Override
            public boolean update(SceneContext c) {
                return condition.test(c);
            }
        };
    }

    /** Ждёт, пока игрок зайдёт в зону. */
    public static Step waitInZone(AABB zone) {
        return waitUntil(c -> zone.contains(c.player.position()));
    }

    public static Step advance(StoryChapter chapter) {
        return run(c -> StoryManager.advanceTo(c.player, chapter));
    }

    // ---------- NPC ----------
    public static Step spawnNpc(String key, NpcType type, double x, double y, double z, float yaw) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            StoryNpcEntity npc = ModEntities.STORY_NPC.get().create(level);
            if (npc == null) {
                return;
            }
            npc.setNpcType(type);
            npc.moveTo(x, y, z, yaw, 0.0F);
            npc.setYHeadRot(yaw);
            npc.setCustomName(Component.translatable("npc.fateapocrypha." + type.id()));
            npc.setCustomNameVisible(true);
            npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(type.heldItem()));
            npc.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            level.addFreshEntity(npc);
            c.npcs.put(key, npc.getUUID());
            SceneManager.register(npc.getUUID());
        });
    }

    public static Step despawnAllNpcs() {
        return run(c -> {
            ServerLevel level = c.trifas();
            for (UUID id : c.npcs.values()) {
                SceneManager.unregister(id);
                if (level != null) {
                    Entity entity = level.getEntity(id);
                    if (entity != null) {
                        entity.discard();
                    }
                }
            }
            c.npcs.clear();
        });
    }

    /**
     * NPC идёт к точке. waitForPlayer: останавливается, если игрок отстал больше чем на 10 блоков.
     * Если NPC дерётся, он не идёт, пока не победит. Застрял - телепортируется на точку.
     */
    public static Step npcWalk(String key, double x, double y, double z, double speed,
                               int timeoutTicks, boolean waitForPlayer) {
        return new Step() {
            private int moving = 0;

            @Override
            public boolean update(SceneContext c) {
                StoryNpcEntity npc = c.npc(key);
                if (npc == null) {
                    return true;
                }
                double dx = npc.getX() - x;
                double dz = npc.getZ() - z;
                if (dx * dx + dz * dz < 2.25D) {
                    return true;
                }
                if (npc.getTarget() != null && npc.getTarget().isAlive()) {
                    return false;
                }
                if (waitForPlayer && c.player.distanceToSqr(npc) > 100.0D) {
                    npc.getNavigation().stop();
                    return false;
                }
                moving++;
                if (moving > timeoutTicks) {
                    npc.moveTo(x, y, z, npc.getYRot(), 0.0F);
                    return true;
                }
                if (moving % 10 == 1) {
                    npc.getNavigation().moveTo(x, y, z, speed);
                }
                return false;
            }
        };
    }

    // ---------- Бой с големами ----------
    /** Появляются големы Ёггдмиллениа и нападают на игрока; шаг ждёт, пока все они будут побеждены. */
    public static Step golemFight(double[][] positions) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                ServerLevel level = c.trifas();
                if (level == null) {
                    return;
                }
                for (double[] p : positions) {
                    IronGolem golem = EntityType.IRON_GOLEM.create(level);
                    if (golem == null) {
                        continue;
                    }
                    golem.moveTo(p[0], p[1], p[2], 90.0F, 0.0F);
                    golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
                    golem.setHealth(40.0F);
                    golem.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0D);
                    golem.addTag("fate_golem");
                    golem.setCustomName(Component.translatable("npc.fateapocrypha.golem"));
                    golem.setTarget(c.player);
                    level.addFreshEntity(golem);
                    c.mobs.add(golem.getUUID());
                }
            }

            @Override
            public boolean update(SceneContext c) {
                ServerLevel level = c.trifas();
                if (level == null) {
                    return true;
                }
                boolean anyAlive = false;
                for (UUID id : c.mobs) {
                    Entity entity = level.getEntity(id);
                    if (entity instanceof IronGolem golem && golem.isAlive()) {
                        anyAlive = true;
                        if (c.stepTicks % 20 == 0
                                && (golem.getTarget() == null || !golem.getTarget().isAlive())) {
                            golem.setTarget(c.player);
                        }
                    }
                }
                return !anyAlive;
            }
        };
    }
}
