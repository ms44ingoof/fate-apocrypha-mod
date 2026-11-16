package com.example.fateapocrypha.entity;

import com.example.fateapocrypha.scene.SceneManager;
import com.example.fateapocrypha.story.NpcType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
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
public class StoryNpcEntity extends PathfinderMob implements AnimatedActor {
    private static final EntityDataAccessor<String> NPC_ID =
            SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> ANIM =
            SynchedEntityData.defineId(StoryNpcEntity.class, EntityDataSerializers.STRING);

    private int animStartAge = 0;
    private int animEnd = 0;

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
        this.entityData.define(ANIM, "");
    }

    // ---------- Анимации действий (cast, bow, kneel, wave...): рисует FateModel ----------
    @Override
    public String getAnim() {
        return this.entityData.get(ANIM);
    }

    @Override
    public int getAnimStartAge() {
        return this.animStartAge;
    }

    /** Запускает действие; ticks = 0 значит держать, пока его не сменят или не вызовут с пустым именем. */
    public void playAnim(String name, int ticks) {
        this.animStartAge = this.tickCount;
        this.animEnd = ticks > 0 ? this.tickCount + ticks : 0;
        this.entityData.set(ANIM, name);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ANIM.equals(key)) {
            this.animStartAge = this.tickCount;
        }
    }

    /** Каждый удар запускает плавную трёхфазную анимацию (замах, удар, возврат). */
    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        super.swing(hand, updateSelf);
        if (!this.level().isClientSide) {
            playAnim("attack", 12);
        }
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
        if (!this.level().isClientSide && this.animEnd > 0 && this.tickCount >= this.animEnd) {
            this.animEnd = 0;
            this.entityData.set(ANIM, "");
        }
    }
}
