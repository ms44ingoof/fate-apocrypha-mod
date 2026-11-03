package com.example.fateapocrypha.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Жанна д'Арк, Ruler. Фантазм: La Pucelle - пламя, которое требует почти всей её жизни. */
public class JeanneEntity extends ServantEntity {
    public JeanneEntity(EntityType<? extends JeanneEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createServantAttributes(44.0D, 0.33D, 6.0D);
    }

    @Override
    public String phantasmNameKey() {
        return "phantasm.fateapocrypha.jeanne";
    }

    @Override
    protected void castPhantasm(ServerLevel level) {
        for (LivingEntity enemy : findEnemies(12.0D)) {
            enemy.hurt(this.damageSources().mobAttack(this), 35.0F);
            enemy.setSecondsOnFire(10);
            level.sendParticles(ParticleTypes.FLAME,
                    enemy.getX(), enemy.getY(0.5D), enemy.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.1D);
        }
        level.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY(0.5D), this.getZ(), 120, 4.0D, 1.0D, 4.0D, 0.15D);
        level.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY(0.5D), this.getZ(), 80, 3.0D, 1.5D, 3.0D, 0.1D);
        // цена фантазма: почти вся жизнь Жанны (она не погибает, но остаётся на грани)
        this.setHealth(Math.min(this.getHealth(), 2.0F));
    }
}
