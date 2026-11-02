package com.example.fateapocrypha.entity;

import com.example.fateapocrypha.scene.SceneManager;
import com.example.fateapocrypha.story.NpcType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Сюжетный NPC (Астольфо, Хирон...). Бессмертен, не бродит сам - им управляют сцены. */
public class StoryNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);

    public StoryNpcEntity(EntityType<? extends StoryNpcEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                e -> e.getTags().contains("fate_golem")));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_ID, NpcType.ASTOLFO.id());
    }

    public NpcType getNpcType() {
        return NpcType.byId(this.entityData.get(NPC_ID));
    }

    public void setNpcType(NpcType type) {
        this.entityData.set(NPC_ID, type.id());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcId", this.entityData.get(NPC_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(NPC_ID, tag.getString("NpcId"));
    }

    /** NPC, потерявший свою сцену (после перезапуска сервера), удаляется. */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 40 && !SceneManager.isManaged(this.getUUID())) {
            this.discard();
        }
    }
}
