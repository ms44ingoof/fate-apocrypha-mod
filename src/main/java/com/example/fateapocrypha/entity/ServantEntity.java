package com.example.fateapocrypha.entity;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Общая основа всех Слуг: ИИ, усиление (120 с) и Благородный Фантазм (600 с). */
public abstract class ServantEntity extends TamableAnimal {
    public static final int BUFF_COOLDOWN_TICKS = 120 * 20;
    public static final int PHANTASM_COOLDOWN_TICKS = 600 * 20;
    public static final int TELEPORT_COOLDOWN_TICKS = 15 * 20;

    private static final EntityDataAccessor<Integer> BUFF_READY =
            SynchedEntityData.defineId(ServantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHANTASM_READY =
            SynchedEntityData.defineId(ServantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TELEPORT_READY =
            SynchedEntityData.defineId(ServantEntity.class, EntityDataSerializers.INT);

    protected ServantEntity(EntityType<? extends ServantEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createServantAttributes(double health, double speed, double damage) {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Ключ перевода названия Благородного Фантазма. */
    public abstract String phantasmNameKey();

    /** Сам эффект Благородного Фантазма (вызывается на сервере). */
    protected abstract void castPhantasm(ServerLevel level);

    // ---------- ИИ ----------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1D, 8.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, false));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e.getTags().contains("fate_golem")));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !this.isOwnedBy(target) && super.canAttack(target);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    // ---------- Перезарядки ----------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BUFF_READY, 0);
        this.entityData.define(PHANTASM_READY, 0);
        this.entityData.define(TELEPORT_READY, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BuffReady", this.entityData.get(BUFF_READY));
        tag.putInt("PhantasmReady", this.entityData.get(PHANTASM_READY));
        tag.putInt("TeleportReady", this.entityData.get(TELEPORT_READY));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(BUFF_READY, tag.getInt("BuffReady"));
        this.entityData.set(PHANTASM_READY, tag.getInt("PhantasmReady"));
        this.entityData.set(TELEPORT_READY, tag.getInt("TeleportReady"));
    }

    /** Оставшиеся тики перезарядки усиления (работает и на клиенте). */
    public int getBuffRemaining() {
        return Math.max(0, this.entityData.get(BUFF_READY) - (int) this.level().getGameTime());
    }

    /** Оставшиеся тики перезарядки Фантазма (работает и на клиенте). */
    public int getPhantasmRemaining() {
        return Math.max(0, this.entityData.get(PHANTASM_READY) - (int) this.level().getGameTime());
    }

    /** Оставшиеся тики перезарядки телепорта Слуги к Мастеру (работает и на клиенте). */
    public int getTeleportRemaining() {
        return Math.max(0, this.entityData.get(TELEPORT_READY) - (int) this.level().getGameTime());
    }

    public void startTeleportCooldown() {
        this.entityData.set(TELEPORT_READY, (int) this.level().getGameTime() + TELEPORT_COOLDOWN_TICKS);
    }

    public void sendCooldownMessage(ServerPlayer master, int remainingTicks) {
        sendCooldown(master, remainingTicks);
    }

    private void sendCooldown(ServerPlayer master, int remainingTicks) {
        master.displayClientMessage(
                Component.translatable("message.fateapocrypha.cooldown", (remainingTicks + 19) / 20), true);
    }

    // ---------- Действия Командного заклинания ----------
    public boolean useBuff(ServerPlayer master) {
        int remaining = getBuffRemaining();
        if (remaining > 0) {
            sendCooldown(master, remaining);
            return false;
        }
        this.entityData.set(BUFF_READY, (int) this.level().getGameTime() + BUFF_COOLDOWN_TICKS);

        this.heal(this.getMaxHealth());
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY(0.5D), this.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.05D);
        }
        master.displayClientMessage(Component.translatable("message.fateapocrypha.buff_used"), true);
        return true;
    }

    public boolean usePhantasm(ServerPlayer master) {
        int remaining = getPhantasmRemaining();
        if (remaining > 0) {
            sendCooldown(master, remaining);
            return false;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        this.entityData.set(PHANTASM_READY, (int) this.level().getGameTime() + PHANTASM_COOLDOWN_TICKS);
        castPhantasm(serverLevel);
        master.displayClientMessage(Component.translatable("message.fateapocrypha.phantasm_used",
                Component.translatable(phantasmNameKey())), true);
        return true;
    }

    // ---------- Помощники для Фантазмов ----------
    protected List<LivingEntity> findEnemies(double radius) {
        return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                e -> e != this && e.isAlive() && !this.isOwnedBy(e)
                        && (e instanceof Enemy || e == this.getTarget()));
    }

    protected void visualLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }
}
