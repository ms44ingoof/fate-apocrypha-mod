package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModEntities;
import com.example.fateapocrypha.entity.ServantEntity;
import com.example.fateapocrypha.entity.StoryCameraEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import com.example.fateapocrypha.network.CameraPacket;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LightningBolt;
import com.example.fateapocrypha.entity.StoryNpcEntity;
import com.example.fateapocrypha.network.DialoguePacket;
import com.example.fateapocrypha.network.FadePacket;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.servant.ServantActions;
import com.example.fateapocrypha.servant.ServantType;
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
                return c.stepTicks >= ticks || c.consumeSkip();
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
            if (type.heldItem() != net.minecraft.world.item.Items.AIR) {
                npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(type.heldItem()));
                npc.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
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
        return golemFight(positions, 0, Integer.MAX_VALUE);
    }

    /** Бой заканчивается, когда големов осталось не больше stopAtMostAlive (или прошло maxTicks тиков). */
    public static Step golemFight(double[][] positions, int stopAtMostAlive, int maxTicks) {
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
                int alive = 0;
                for (UUID id : c.mobs) {
                    Entity entity = level.getEntity(id);
                    if (entity instanceof IronGolem golem && golem.isAlive()) {
                        alive++;
                        if (c.stepTicks % 20 == 0
                                && (golem.getTarget() == null || !golem.getTarget().isAlive())) {
                            golem.setTarget(c.player);
                        }
                    }
                }
                return alive <= stopAtMostAlive || c.stepTicks > maxTicks;
            }
        };
    }

    // ---------- Режиссура: составные шаги ----------
    /** Выполняет шаги по очереди (со своим отсчётом тиков). */
    public static Step sequence(List<Step> steps) {
        return new Step() {
            private int index = 0;
            private boolean began = false;
            private int local = 0;

            @Override
            public boolean update(SceneContext c) {
                int saved = c.stepTicks;
                while (index < steps.size()) {
                    Step step = steps.get(index);
                    c.stepTicks = local;
                    if (!began) {
                        step.begin(c);
                        began = true;
                    }
                    if (step.update(c)) {
                        index++;
                        began = false;
                        local = 0;
                    } else {
                        local++;
                        break;
                    }
                }
                c.stepTicks = saved;
                return index >= steps.size();
            }
        };
    }

    /** Выполняет шаги одновременно; заканчивается, когда закончили все. */
    public static Step parallel(Step... steps) {
        return new Step() {
            private final boolean[] done = new boolean[steps.length];
            private boolean began = false;

            @Override
            public boolean update(SceneContext c) {
                if (!began) {
                    for (Step step : steps) {
                        step.begin(c);
                    }
                    began = true;
                }
                boolean all = true;
                for (int i = 0; i < steps.length; i++) {
                    if (!done[i]) {
                        done[i] = steps[i].update(c);
                    }
                    if (!done[i]) {
                        all = false;
                    }
                }
                return all;
            }
        };
    }

    // ---------- Кино: камера от третьего лица ----------
    /** Рассчитывает поворот камеры (yaw, pitch) так, чтобы она смотрела из pos на точку look. */
    public static float[] aim(Vec3 pos, Vec3 look) {
        double dx = look.x - pos.x;
        double dy = look.y - pos.y;
        double dz = look.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(-dx, dz) * 180.0D / Math.PI);
        float pitch = (float) (-Math.atan2(dy, horizontal) * 180.0D / Math.PI);
        return new float[] {yaw, pitch};
    }

    /** Включает кино-режим: игрок становится невидимым, неподвижным и неуязвимым, вид переходит на камеру. */
    public static Step cinematicStart(Vec3 pos, Vec3 look) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            StoryCameraEntity camera = ModEntities.STORY_CAMERA.get().create(level);
            if (camera == null) {
                return;
            }
            float[] rot = aim(pos, look);
            camera.moveTo(pos.x, pos.y, pos.z, rot[0], rot[1]);
            level.addFreshEntity(camera);
            c.cameraId = camera.getUUID();
            c.cinematic = true;
            c.player.setInvulnerable(true);
            c.player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 24000, 0, false, false));
            c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24000, 10, false, false));
            c.player.addEffect(new MobEffectInstance(MobEffects.JUMP, 24000, 200, false, false));
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> c.player),
                    new CameraPacket(camera.getId(), true));
        });
    }

    public static Step cinematicEnd() {
        return run(SceneContext::endCinematic);
    }

    /** Плавный пролёт камеры: from -> to за ticks тиков; камера всё время смотрит на точку look. */
    public static Step cameraMove(Vec3 fromPos, Vec3 fromLook, Vec3 toPos, Vec3 toLook, int ticks) {
        return new Step() {
            @Override
            public boolean update(SceneContext c) {
                StoryCameraEntity camera = c.camera();
                if (camera == null) {
                    return true;
                }
                double t = ticks <= 0 ? 1.0D : Math.min(1.0D, c.stepTicks / (double) ticks);
                t = t * t * (3.0D - 2.0D * t);
                Vec3 pos = fromPos.lerp(toPos, t);
                Vec3 look = fromLook.lerp(toLook, t);
                float[] rot = aim(pos, look);
                camera.setPos(pos.x, pos.y, pos.z);
                camera.setYRot(rot[0]);
                camera.setXRot(rot[1]);
                return c.stepTicks >= ticks;
            }
        };
    }

    // ---------- Режиссура: актёры и эффекты ----------
    public static Step npcFace(String key, float yaw) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            if (npc != null) {
                npc.setYRot(yaw);
                npc.setYHeadRot(yaw);
                npc.yBodyRot = yaw;
            }
        });
    }

    public static Step npcSwing(String key) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            if (npc != null) {
                npc.swing(InteractionHand.MAIN_HAND);
            }
        });
    }

    /** Анимация получения удара (красная вспышка), NPC при этом не страдает. */
    public static Step npcHurt(String key) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            ServerLevel level = c.trifas();
            if (npc != null && level != null) {
                level.broadcastEntityEvent(npc, (byte) 2);
            }
        });
    }

    /** Гибель героя: души, звук и исчезновение. */
    public static Step npcDie(String key) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            ServerLevel level = c.trifas();
            if (npc == null || level == null) {
                return;
            }
            level.sendParticles(ParticleTypes.SOUL, npc.getX(), npc.getY(0.5D), npc.getZ(),
                    50, 0.3D, 0.8D, 0.3D, 0.05D);
            level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY(0.5D), npc.getZ(),
                    30, 0.4D, 0.9D, 0.4D, 0.08D);
            level.playSound(null, BlockPos.containing(npc.position()), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.NEUTRAL, 1.5F, 1.0F);
            SceneManager.unregister(npc.getUUID());
            c.npcs.remove(key);
            npc.discard();
        });
    }

    public static Step lightning(double x, double y, double z) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(x, y, z);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        });
    }

    public static Step particles(ParticleOptions particle, double x, double y, double z, int count, double spread) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level != null) {
                level.sendParticles(particle, x, y, z, count, spread, spread, spread, 0.1D);
            }
        });
    }

    // ---------- Сцена с Зигфридом: смертельная рана и сердце ----------
    /** Убирает оставшихся мобов сцены (дымом). */
    public static Step discardMobs() {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            for (UUID id : c.mobs) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY(0.5D), entity.getZ(),
                            30, 0.4D, 0.6D, 0.4D, 0.05D);
                    entity.discard();
                }
            }
            c.mobs.clear();
        });
    }

    /** NPC уходит (дым и звук), без драматичной гибели. */
    public static Step npcLeave(String key) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            ServerLevel level = c.trifas();
            if (npc == null || level == null) {
                return;
            }
            level.sendParticles(ParticleTypes.POOF, npc.getX(), npc.getY(0.5D), npc.getZ(),
                    40, 0.4D, 0.8D, 0.4D, 0.05D);
            level.playSound(null, BlockPos.containing(npc.position()), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
            SceneManager.unregister(npc.getUUID());
            c.npcs.remove(key);
            npc.discard();
        });
    }

    public static Step lightningAtPlayer() {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(c.player.getX(), c.player.getY(), c.player.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        });
    }

    /** Смертельная рана: почти ноль здоровья, слабость и защита от случайной гибели во время сцены. */
    public static Step hurtPlayerNearDeath() {
        return run(c -> {
            c.player.setHealth(2.0F);
            c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 3, false, false));
            c.player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            c.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 4, false, false));
            c.player.level().broadcastEntityEvent(c.player, (byte) 2);
        });
    }

    /** Сердце Зигфрида: игрок исцелён, получает регенерацию и постоянную стойкость. */
    public static Step healPlayer() {
        return run(c -> {
            c.player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            c.player.removeEffect(MobEffects.BLINDNESS);
            c.player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            c.player.setHealth(c.player.getMaxHealth());
            c.player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1, false, false));
            ServerLevel level = c.trifas();
            if (level != null) {
                level.sendParticles(ParticleTypes.HEART, c.player.getX(), c.player.getY(1.0D), c.player.getZ(),
                        20, 0.5D, 0.8D, 0.5D, 0.05D);
                level.sendParticles(ParticleTypes.END_ROD, c.player.getX(), c.player.getY(1.0D), c.player.getZ(),
                        60, 0.6D, 1.0D, 0.6D, 0.1D);
            }
        });
    }

    /** Игрок становится Мастером Слуги: тот появляется рядом и следует за игроком. */
    public static Step bindServant(ServantType type) {
        return run(c -> ServantActions.spawnServant(c.player, type));
    }

    /** Частицы на текущей позиции NPC (свечение печатей, удар клинка). */
    public static Step particlesAtNpc(String key, ParticleOptions particle, int count) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            ServerLevel level = c.trifas();
            if (npc != null && level != null) {
                level.sendParticles(particle, npc.getX(), npc.getY(0.6D), npc.getZ(),
                        count, 0.4D, 0.7D, 0.4D, 0.08D);
            }
        });
    }

    // ---------- Слуга игрока: убрать и вернуть при переходах между сценами ----------
    public static Step dismissServant() {
        return run(c -> {
            ServantEntity servant = ServantActions.findServant(c.player);
            if (servant != null) {
                if (servant.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.POOF, servant.getX(), servant.getY(0.5D), servant.getZ(),
                            20, 0.3D, 0.6D, 0.3D, 0.05D);
                }
                servant.discard();
            }
        });
    }

    public static Step restoreServant(ServantType type) {
        return run(c -> {
            if (ServantActions.findServant(c.player) == null) {
                ServantActions.spawnServant(c.player, type);
            }
        });
    }

    public static Step npcTeleport(String key, double x, double y, double z, float yaw) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            if (npc != null) {
                npc.moveTo(x, y, z, yaw, 0.0F);
                npc.setYHeadRot(yaw);
                npc.getNavigation().stop();
            }
        });
    }

    // ---------- Бои сцен ----------
    /** Тени (призрачные существа иллюзии): нападают на игрока, шаг ждёт, пока все побеждены. */
    public static Step shadeFight(double[][] positions) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                ServerLevel level = c.trifas();
                if (level == null) {
                    return;
                }
                for (double[] p : positions) {
                    Vex shade = EntityType.VEX.create(level);
                    if (shade == null) {
                        continue;
                    }
                    shade.moveTo(p[0], p[1], p[2], 90.0F, 0.0F);
                    shade.addTag("fate_golem");
                    shade.setCustomName(Component.translatable("npc.fateapocrypha.shade"));
                    shade.setTarget(c.player);
                    level.addFreshEntity(shade);
                    c.mobs.add(shade.getUUID());
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
                    if (entity instanceof Vex shade && shade.isAlive()) {
                        anyAlive = true;
                        if (c.stepTicks % 20 == 0 && (shade.getTarget() == null || !shade.getTarget().isAlive())) {
                            shade.setTarget(c.player);
                        }
                    }
                }
                return !anyAlive || c.stepTicks > 2400;
            }
        };
    }

    /** Босс-голем: много здоровья и сильный удар; шаг ждёт его гибели. */
    public static Step bossFight(double[][] positions, double health, double damage, String nameKey) {
        return new Step() {
            @Override
            public void begin(SceneContext c) {
                ServerLevel level = c.trifas();
                if (level == null) {
                    return;
                }
                for (double[] p : positions) {
                    IronGolem boss = EntityType.IRON_GOLEM.create(level);
                    if (boss == null) {
                        continue;
                    }
                    boss.moveTo(p[0], p[1], p[2], 0.0F, 0.0F);
                    boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                    boss.setHealth((float) health);
                    boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
                    boss.addTag("fate_golem");
                    boss.setCustomName(Component.translatable(nameKey));
                    boss.setCustomNameVisible(true);
                    boss.setTarget(c.player);
                    level.addFreshEntity(boss);
                    c.mobs.add(boss.getUUID());
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
                    if (entity instanceof IronGolem boss && boss.isAlive()) {
                        anyAlive = true;
                        if (c.stepTicks % 20 == 0 && (boss.getTarget() == null || !boss.getTarget().isAlive())) {
                            boss.setTarget(c.player);
                        }
                    }
                }
                return !anyAlive || c.stepTicks > 6000;
            }
        };
    }

    /** Корова, овца, курица по очереди: оживляют ферму. Исчезают в конце сцены. */
    public static Step spawnAnimals(double[][] positions) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            int i = 0;
            for (double[] p : positions) {
                net.minecraft.world.entity.Mob animal = switch (i++ % 3) {
                    case 0 -> EntityType.COW.create(level);
                    case 1 -> EntityType.SHEEP.create(level);
                    default -> EntityType.CHICKEN.create(level);
                };
                if (animal == null) {
                    continue;
                }
                animal.moveTo(p[0], p[1], p[2], (float) ((i * 70) % 360), 0.0F);
                level.addFreshEntity(animal);
                c.mobs.add(animal.getUUID());
            }
        });
    }

    /** Меняет отображаемое имя NPC. */
    public static Step npcRename(String key, String nameKey) {
        return run(c -> {
            StoryNpcEntity npc = c.npc(key);
            if (npc != null) {
                npc.setCustomName(Component.translatable(nameKey));
            }
        });
    }

    // ================= Финальные главы: боссы, силы Зига, дракон =================

    /**
     * Бой с уже созданным сюжетным NPC (spawnNpc с тем же key). NPC на время боя становится уязвим.
     * Бой заканчивается, когда здоровье падает до endFraction от максимума (NPC не погибает, а снова
     * становится неуязвимым: его гибель играет сцена). resetHealth=false продолжает бой со второй фазы.
     * Каждые specialEvery тиков босс помечает точку под игроком: через 2,5 с там бьёт молния (8 урона).
     */
    public static Step npcBoss(String key, double health, double damage, double endFraction, boolean resetHealth,
                               int specialEvery, String specialKey, String nameKey, BossEvent.BossBarColor color) {
        return new Step() {
            private ServerBossEvent bar;
            private int sinceSpecial = 0;
            private int charge = 0;
            private Vec3 mark = Vec3.ZERO;

            @Override
            public void begin(SceneContext c) {
                StoryNpcEntity npc = c.npc(key);
                if (npc == null) {
                    return;
                }
                if (resetHealth) {
                    npc.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                    npc.setHealth((float) health);
                }
                npc.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
                npc.setInvulnerable(false);
                npc.setTarget(c.player);
                bar = new ServerBossEvent(Component.translatable(nameKey), color, BossEvent.BossBarOverlay.PROGRESS);
                bar.addPlayer(c.player);
            }

            private void finish(StoryNpcEntity npc) {
                if (bar != null) {
                    bar.removeAllPlayers();
                    bar = null;
                }
                if (npc != null) {
                    npc.setInvulnerable(true);
                    npc.setTarget(null);
                    npc.getNavigation().stop();
                }
            }

            @Override
            public boolean update(SceneContext c) {
                StoryNpcEntity npc = c.npc(key);
                ServerLevel level = c.trifas();
                if (npc == null || !npc.isAlive() || level == null || c.stepTicks > 12000) {
                    finish(npc);
                    return true;
                }
                float max = npc.getMaxHealth();
                if (npc.getHealth() <= max * endFraction) {
                    npc.setHealth(Math.max(1.0F, (float) (max * endFraction)));
                    finish(npc);
                    return true;
                }
                if (bar != null) {
                    bar.setProgress(Math.max(0.0F, Math.min(1.0F, npc.getHealth() / max)));
                }
                if (c.stepTicks % 20 == 0 && (npc.getTarget() == null || !npc.getTarget().isAlive())) {
                    npc.setTarget(c.player);
                }
                if (specialEvery > 0) {
                    if (charge > 0) {
                        charge--;
                        if (charge % 4 == 0) {
                            level.sendParticles(ParticleTypes.FLAME, mark.x, mark.y + 0.2D, mark.z,
                                    14, 1.6D, 0.05D, 1.6D, 0.01D);
                            level.sendParticles(ParticleTypes.END_ROD, mark.x, mark.y + 1.5D, mark.z,
                                    4, 0.3D, 1.0D, 0.3D, 0.02D);
                        }
                        if (charge == 0) {
                            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                            if (bolt != null) {
                                bolt.moveTo(mark.x, mark.y, mark.z);
                                bolt.setVisualOnly(true);
                                level.addFreshEntity(bolt);
                            }
                            level.sendParticles(ParticleTypes.EXPLOSION, mark.x, mark.y + 1.0D, mark.z,
                                    6, 1.0D, 0.5D, 1.0D, 0.0D);
                            level.playSound(null, BlockPos.containing(mark), SoundEvents.GENERIC_EXPLODE,
                                    SoundSource.HOSTILE, 1.5F, 1.0F);
                            AABB area = new AABB(mark.x - 3.0D, mark.y - 1.0D, mark.z - 3.0D,
                                    mark.x + 3.0D, mark.y + 3.0D, mark.z + 3.0D);
                            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                                    t -> t != npc && t.isAlive() && !(t instanceof StoryNpcEntity))) {
                                target.hurt(npc.damageSources().magic(), 8.0F);
                            }
                        }
                    } else {
                        sinceSpecial++;
                        if (sinceSpecial >= specialEvery) {
                            sinceSpecial = 0;
                            charge = 50;
                            mark = c.player.position();
                            c.player.displayClientMessage(
                                    Component.translatable(specialKey).withStyle(ChatFormatting.RED), true);
                            npc.swing(InteractionHand.MAIN_HAND);
                            level.playSound(null, BlockPos.containing(mark), SoundEvents.BEACON_POWER_SELECT,
                                    SoundSource.HOSTILE, 1.5F, 0.8F);
                        }
                    }
                }
                return false;
            }
        };
    }

    /** Сердце Зигфрида: на время боя сила, защита и регенерация. */
    public static Step siegfriedHeart(int ticks) {
        return run(c -> {
            c.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1, false, true));
            c.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, false, true));
            c.player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
            ServerLevel level = c.trifas();
            if (level != null) {
                level.sendParticles(ParticleTypes.END_ROD, c.player.getX(), c.player.getY(1.0D), c.player.getZ(),
                        60, 0.6D, 1.0D, 0.6D, 0.1D);
            }
            c.player.level().playSound(null, c.player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        });
    }

    /** Сила Франкенштейн: молния бьёт в Зига, но не вредит ему, и даёт скорость и ещё больше силы. */
    public static Step frankenSpark(int ticks) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level != null) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(c.player.getX(), c.player.getY(), c.player.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.player.getX(), c.player.getY(1.0D),
                        c.player.getZ(), 80, 0.7D, 1.0D, 0.7D, 0.3D);
            }
            c.player.removeEffect(MobEffects.DAMAGE_BOOST);
            c.player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 2, false, true));
            c.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1, false, true));
            c.player.setHealth(c.player.getMaxHealth());
        });
    }

    /** Дракон Фафнир (огромный фантом без ИИ). Убирается шагом discardMobs. */
    public static Step spawnDragon(double x, double y, double z, float yaw) {
        return run(c -> {
            ServerLevel level = c.trifas();
            if (level == null) {
                return;
            }
            Phantom dragon = EntityType.PHANTOM.create(level);
            if (dragon == null) {
                return;
            }
            dragon.moveTo(x, y, z, yaw, 0.0F);
            dragon.setPhantomSize(30);
            dragon.setNoAi(true);
            dragon.setInvulnerable(true);
            dragon.setSilent(true);
            dragon.setPersistenceRequired();
            dragon.setCustomName(Component.translatable("npc.fateapocrypha.fafnir"));
            dragon.setCustomNameVisible(true);
            level.addFreshEntity(dragon);
            c.mobs.add(dragon.getUUID());
        });
    }
}
