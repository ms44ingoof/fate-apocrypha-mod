package com.example.fateapocrypha.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Могред, Saber Красных. Фантазм: Clarent Blood Arthur. */
public class MordredEntity extends ServantEntity {
    public MordredEntity(EntityType<? extends MordredEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createServantAttributes(50.0D, 0.35D, 9.0D);
    }

    @Override
    public String phantasmNameKey() {
        return "phantasm.fateapocrypha.mordred";
    }

    @Override
    protected void castPhantasm(ServerLevel level) {
        for (LivingEntity enemy : findEnemies(10.0D)) {
            enemy.hurt(this.damageSources().mobAttack(this), 30.0F);
            enemy.knockback(1.2D, this.getX() - enemy.getX(), this.getZ() - enemy.getZ());
            visualLightning(level, enemy.getX(), enemy.getY(), enemy.getZ());
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY(0.5D), this.getZ(), 60, 2.0D, 1.0D, 2.0D, 0.2D);
    }
}
