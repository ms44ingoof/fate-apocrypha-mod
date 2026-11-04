package com.example.fateapocrypha.entity;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Астольфо, Rider Чёрных. Фантазм: Trap of Argalia. */
public class AstolfoEntity extends ServantEntity {
    public AstolfoEntity(EntityType<? extends AstolfoEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createServantAttributes(100.0D, 0.34D, 9.0D);
    }

    @Override
    public String phantasmNameKey() {
        return "phantasm.fateapocrypha.astolfo";
    }

    @Override
    protected void castPhantasm(ServerLevel level) {
        List<LivingEntity> enemies = findEnemies(7.0D);
        for (LivingEntity enemy : enemies) {
            enemy.hurt(this.damageSources().mobAttack(this), 12.0F);
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            enemy.setDeltaMovement(enemy.getDeltaMovement().add(0.0D, 1.0D, 0.0D));
            enemy.hurtMarked = true;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    enemy.getX(), enemy.getY(0.5D), enemy.getZ(), 20, 0.4D, 0.5D, 0.4D, 0.1D);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY(0.5D), this.getZ(), 40, 1.5D, 0.8D, 1.5D, 0.05D);
    }
}
