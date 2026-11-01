package com.example.fateapocrypha.item;

import com.example.fateapocrypha.entity.AstolfoEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Командное заклинание: 3 заряда. ПКМ по своему Слуге усиливает его. */
public class CommandSealItem extends Item {
    public CommandSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof AstolfoEntity servant && servant.isOwnedBy(player)) {
            if (!player.level().isClientSide) {
                servant.heal(servant.getMaxHealth());
                servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 2));
                servant.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
                servant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            servant.getX(), servant.getY(0.5D), servant.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.05D);
                }
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
