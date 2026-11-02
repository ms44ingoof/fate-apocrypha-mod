package com.example.fateapocrypha.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Семирамида, Assassin Красных. Фантазм: Hanging Gardens of Babylon. */
public class SemiramisEntity extends ServantEntity {
    public SemiramisEntity(EntityType<? extends SemiramisEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createServantAttributes(36.0D, 0.33D, 6.0D);
    }

    @Override
    public String phantasmNameKey() {
        return "phantasm.fateapocrypha.semiramis";
    }

    @Override
    protected void castPhantasm(ServerLevel level) {
        for (LivingEntity enemy : findEnemies(14.0D)) {
            enemy.hurt(this.damageSources().magic(), 10.0F);
            enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    enemy.getX(), enemy.getY(0.5D), enemy.getZ(), 25, 0.4D, 0.6D, 0.4D, 0.05D);
        }
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
        level.sendParticles(ParticleTypes.WITCH,
                this.getX(), this.getY(0.5D), this.getZ(), 80, 3.0D, 1.0D, 3.0D, 0.1D);
    }
}
