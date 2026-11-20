package com.example.fateapocrypha.item;

import com.example.fateapocrypha.entity.StoryNpcEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Легендарное оружие героев. ПКМ запускает приказ оружия (Благородный Фантазм) с собственной перезарядкой,
 * оружие светится, чинится святой сталью.
 */
public class FateWeaponItem extends SwordItem {
    public enum Ability {
        DRAGON_SLASH("dragon_slash", 12),
        BLOOD_ARTHUR("blood_arthur", 40),
        VASAVI("vasavi", 45),
        ARGALIA("argalia", 10),
        BANNER("banner", 60);

        private final String id;
        private final int cooldownSeconds;

        Ability(String id, int cooldownSeconds) {
            this.id = id;
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    private final Ability ability;

    public FateWeaponItem(Tier tier, int damage, float speed, Properties properties, Ability ability) {
        super(tier, damage, speed, properties);
        this.ability = ability;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** Бальмунг: клинок убийцы драконов, дополнительный урон по драконам и фантомам. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (ability == Ability.DRAGON_SLASH && (target instanceof EnderDragon || target instanceof Phantom)) {
            target.hurt(attacker.damageSources().magic(), 6.0F);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            boolean used;
            switch (ability) {
                case DRAGON_SLASH -> used = dragonSlash(server, player);
                case BLOOD_ARTHUR -> used = bloodArthur(server, player);
                case VASAVI -> used = vasavi(server, player);
                case ARGALIA -> used = argalia(server, player);
                default -> used = banner(server, player);
            }
            if (!used) {
                player.displayClientMessage(Component.translatable("message.fateapocrypha.no_target"), true);
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(this, ability.cooldownSeconds * 20);
            stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(hand));
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.fateapocrypha.ability",
                Component.translatable("ability.fateapocrypha." + ability.id), ability.cooldownSeconds).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("ability.fateapocrypha." + ability.id + ".desc").withStyle(ChatFormatting.DARK_AQUA));
    }

    // ---------------------------------------------------------------- приказы
    private static DamageSource source(Player player) {
        return player.damageSources().playerAttack(player);
    }

    /** Подходящая цель: живая, не союзник и не сюжетный NPC. */
    private static boolean valid(LivingEntity target, Player player) {
        return target != player && target.isAlive() && !(target instanceof Player) && !(target instanceof StoryNpcEntity)
                && !(target instanceof TamableAnimal tamable && tamable.isOwnedBy(player));
    }

    /** Бальмунг: широкий рассекающий удар перед собой (до 7 блоков, угол около 120 градусов). */
    private boolean dragonSlash(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0.0D, look.z);
        dir = dir.lengthSqr() < 1.0e-4D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
        AABB box = player.getBoundingBox().inflate(7.0D, 2.0D, 7.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, t -> valid(t, player))) {
            Vec3 to = e.position().subtract(player.position());
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            double dist = flat.length();
            if (dist < 0.1D || dist > 7.0D || flat.normalize().dot(dir) < 0.45D) {
                continue;
            }
            float damage = 10.0F + (e instanceof EnderDragon || e instanceof Phantom ? 8.0F : 0.0F);
            e.hurt(source(player), damage);
            e.knockback(0.9D, -dir.x, -dir.z);
        }
        Vec3 base = player.position();
        for (int d = 1; d <= 6; d++) {
            double x = base.x + dir.x * d;
            double z = base.z + dir.z * d;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, base.y + 1.0D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, x, base.y + 1.0D, z, 4, 0.5D, 0.4D, 0.5D, 0.02D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
        return true;
    }

    /** Кларент: красный луч на 20 блоков, бьёт всех на линии. */
    private boolean bloodArthur(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Set<LivingEntity> hit = new HashSet<>();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.86F, 0.1F, 0.16F), 1.5F);
        for (double d = 1.0D; d <= 20.0D; d += 0.5D) {
            Vec3 p = eye.add(look.scale(d));
            BlockPos bp = BlockPos.containing(p);
            if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                break;
            }
            level.sendParticles(dust, p.x, p.y, p.z, 3, 0.15D, 0.15D, 0.15D, 0.0D);
            if (((int) (d * 2.0D)) % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.2D, 0.2D, 0.2D, 0.05D);
            }
            AABB around = new AABB(p.x - 0.9D, p.y - 0.9D, p.z - 0.9D, p.x + 0.9D, p.y + 0.9D, p.z + 0.9D);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, around, t -> valid(t, player))) {
                if (hit.add(e)) {
                    e.hurt(source(player), 14.0F);
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    /** Васави Шакти: молния бьёт цель в прицеле (до 32 блоков) и тех, кто рядом с ней. */
    private boolean vasavi(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        LivingEntity target = null;
        double best = 0.0D;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32.0D), t -> valid(t, player))) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist > 32.0D || dist < 1.0D) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot < 0.96D) {
                continue;
            }
            double score = dot - dist * 0.002D;
            if (target == null || score > best) {
                target = e;
                best = score;
            }
        }
        if (target == null) {
            return false;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        target.hurt(source(player), 18.0F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0D), t -> valid(t, player))) {
            if (e != target) {
                e.hurt(source(player), 8.0F);
            }
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0D, target.getZ(), 60, 0.8D, 1.0D, 0.8D, 0.3D);
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    /** Золотое копьё Астольфо: рывок вперёд, сбивающий тех, кто на пути. */
    private boolean argalia(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, Math.max(look.y * 0.4D, 0.1D), look.z).normalize().scale(1.7D);
        Vec3 start = player.position();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        horizontal = horizontal.lengthSqr() < 1.0e-4D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
        AABB path = new AABB(start, start.add(horizontal.scale(6.0D))).inflate(1.6D, 1.0D, 1.6D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, path, t -> valid(t, player))) {
            e.hurt(source(player), 8.0F);
            e.knockback(1.1D, -horizontal.x, -horizontal.z);
        }
        for (int i = 0; i < 12; i++) {
            Vec3 p = start.add(horizontal.scale(i * 0.5D));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0D, p.z, 3, 0.2D, 0.3D, 0.2D, 0.02D);
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1.0D, p.z, 2, 0.2D, 0.3D, 0.2D, 0.1D);
        }
        player.setDeltaMovement(dash.x, dash.y, dash.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    /** Знамя Жанны: исцеляет и защищает всех союзников в радиусе 12 блоков. */
    private boolean banner(ServerLevel level, Player player) {
        AABB box = player.getBoundingBox().inflate(12.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                t -> t.isAlive() && (t instanceof Player || (t instanceof TamableAnimal tamable && tamable.isOwnedBy(player))))) {
            e.heal(8.0F);
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 1.5D, e.getZ(), 6, 0.4D, 0.4D, 0.4D, 0.02D);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, e.getX(), e.getY() + 1.0D, e.getZ(), 12, 0.5D, 0.7D, 0.5D, 0.15D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        return true;
    }
}
