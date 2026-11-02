package com.example.fateapocrypha.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Жанна д'Арк, Ruler. Фантазм: Luminosite Eternelle (защита Мастера и Слуги). */
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
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(this);
        LivingEntity owner = this.getOwner();
        if (owner != null && owner.distanceToSqr(this) < 20.0D * 20.0D) {
            allies.add(owner);
        }
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 4));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
            level.sendParticles(ParticleTypes.END_ROD,
                    ally.getX(), ally.getY(0.5D), ally.getZ(), 50, 0.6D, 1.0D, 0.6D, 0.05D);
        }
    }
}
