package com.example.fateapocrypha.item;

import com.example.fateapocrypha.entity.StoryNpcEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
        BANNER("banner", 60),
        BLACK_KEYS("black_keys", 8),
        IMPALE("impale", 30),
        HEEL("heel", 40),
        PHOEBUS("phoebus", 35),
        GARDENS("gardens", 45),
        FOG("fog", 25),
        REBEL("rebel", 50);

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
            target.invulnerableTime = 0;   // основной удар уже включил неуязвимость: без сброса добавка не пройдёт
            target.hurt(attacker.damageSources().magic(), 6.0F);
        }
        // Нож Джека: удар в спину добавляет урон
        if (ability == Ability.FOG && target.level() instanceof ServerLevel server) {
            Vec3 facing = target.getLookAngle();
            Vec3 toAttacker = attacker.position().subtract(target.position());
            if (toAttacker.lengthSqr() > 1.0e-4D && facing.x * toAttacker.x + facing.z * toAttacker.z < 0.0D) {
                target.invulnerableTime = 0;
                target.hurt(attacker.damageSources().magic(), 4.0F);
                server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.2D);
            }
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
                case BLACK_KEYS -> used = blackKeys(server, player);
                case IMPALE -> used = impale(server, player);
                case HEEL -> used = heel(server, player);
                case PHOEBUS -> used = phoebus(server, player);
                case GARDENS -> used = gardens(server, player);
                case FOG -> used = fog(server, player);
                case REBEL -> used = rebel(server, player);
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

    // ---------------------------------------------------------------- оружие Слуг Чёрных и Красных
    /** Чёрные ключи Широ: веер из трёх клинков, каждый бьёт первую цель на линии (до 24 блоков). */
    private boolean blackKeys(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.45F, 0.25F, 0.7F), 1.1F);
        Set<LivingEntity> hit = new HashSet<>();
        for (int k = -1; k <= 1; k++) {
            Vec3 dir = look.yRot((float) Math.toRadians(k * 7.0D));
            for (double d = 1.0D; d <= 24.0D; d += 0.5D) {
                Vec3 p = eye.add(dir.scale(d));
                BlockPos bp = BlockPos.containing(p);
                if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 4, 0.1D, 0.1D, 0.1D, 0.1D);
                    break;
                }
                level.sendParticles(dust, p.x, p.y, p.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                AABB around = new AABB(p.x - 0.6D, p.y - 0.6D, p.z - 0.6D, p.x + 0.6D, p.y + 0.6D, p.z + 0.6D);
                LivingEntity struck = null;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, around, t -> valid(t, player))) {
                    if (!hit.contains(e)) {
                        struck = e;
                        break;
                    }
                }
                if (struck != null) {
                    hit.add(struck);
                    struck.invulnerableTime = 0;
                    struck.hurt(source(player), 7.0F);
                    level.sendParticles(ParticleTypes.CRIT, struck.getX(), struck.getY() + 1.0D, struck.getZ(), 8, 0.3D, 0.4D, 0.3D, 0.2D);
                    break;
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8F, 1.3F);
        return true;
    }

    /** Казыклы-бей Влада: колья вырастают из земли вокруг владельца (радиус 6), враги ранены, замедлены и подброшены. */
    private boolean impale(ServerLevel level, Player player) {
        Vec3 base = player.position();
        BlockParticleOption stake = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());
        DustParticleOptions blood = new DustParticleOptions(new Vector3f(0.6F, 0.05F, 0.08F), 1.4F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0D, 2.0D, 6.0D), t -> valid(t, player))) {
            if (e.distanceTo(player) > 6.0F) {
                continue;
            }
            e.hurt(source(player), 9.0F);
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            e.setDeltaMovement(e.getDeltaMovement().x, 0.55D, e.getDeltaMovement().z);
            e.hurtMarked = true;
            level.sendParticles(blood, e.getX(), e.getY() + 1.0D, e.getZ(), 20, 0.3D, 0.6D, 0.3D, 0.02D);
        }
        for (int ring = 2; ring <= 6; ring += 2) {
            int points = 6 + ring * 4;
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2.0D * i / points;
                double x = base.x + Math.cos(a) * ring;
                double z = base.z + Math.sin(a) * ring;
                level.sendParticles(stake, x, base.y + 0.2D, z, 6, 0.1D, 0.5D, 0.1D, 0.15D);
                if ((i + ring) % 3 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, x, base.y + 0.9D, z, 1, 0.05D, 0.4D, 0.05D, 0.02D);
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.PLAYERS, 1.4F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }

    /** Копьё Ахиллеса: неуязвимость героя. Короткая защита и скорость, а врагов рядом отбрасывает. */
    private boolean heel(ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0D, 1.5D, 4.0D), t -> valid(t, player))) {
            e.hurt(source(player), 5.0F);
            e.knockback(1.3D, player.getX() - e.getX(), player.getZ() - e.getZ());
        }
        Vec3 base = player.position();
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2.0D * i / 24.0D;
            level.sendParticles(ParticleTypes.END_ROD, base.x + Math.cos(a) * 3.0D, base.y + 0.3D, base.z + Math.sin(a) * 3.0D,
                    2, 0.05D, 0.3D, 0.05D, 0.02D);
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, base.x, base.y + 1.0D, base.z, 25, 0.6D, 0.8D, 0.6D, 0.2D);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
        return true;
    }

    /** Лук Аталанты: ливень стрел на точку в прицеле (до 40 блоков), радиус 5. */
    private boolean phoebus(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 spot = eye.add(look.scale(40.0D));
        for (double d = 2.0D; d <= 40.0D; d += 0.5D) {
            Vec3 p = eye.add(look.scale(d));
            BlockPos bp = BlockPos.containing(p);
            if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                spot = p.subtract(look.scale(0.5D));
                break;
            }
            AABB around = new AABB(p.x - 0.8D, p.y - 0.8D, p.z - 0.8D, p.x + 0.8D, p.y + 0.8D, p.z + 0.8D);
            if (!level.getEntitiesOfClass(LivingEntity.class, around, t -> valid(t, player)).isEmpty()) {
                spot = p;
                break;
            }
        }
        final Vec3 center = spot;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(5.0D, 4.0D, 5.0D), t -> valid(t, player))) {
            if (e.position().distanceTo(center) <= 5.5D) {
                e.hurt(source(player), 12.0F);
            }
        }
        for (int i = 0; i < 18; i++) {
            double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double r = Math.sqrt(level.getRandom().nextDouble()) * 5.0D;
            double x = center.x + Math.cos(a) * r;
            double z = center.z + Math.sin(a) * r;
            for (int h = 0; h < 8; h++) {
                level.sendParticles(h % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.CRIT, x, center.y + 0.5D + h * 1.0D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            level.sendParticles(ParticleTypes.CRIT, x, center.y + 0.3D, z, 3, 0.2D, 0.1D, 0.2D, 0.15D);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    /** Посох Семирамис: ядовитый сад вокруг владельца (радиус 8), враги ранены, отравлены и замедлены. */
    private boolean gardens(ServerLevel level, Player player) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0D, 3.0D, 8.0D), t -> valid(t, player))) {
            if (e.distanceTo(player) > 8.0F) {
                continue;
            }
            e.hurt(source(player), 6.0F);
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        }
        Vec3 base = player.position();
        for (int ring = 2; ring <= 8; ring += 2) {
            int points = ring * 5;
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2.0D * i / points;
                double x = base.x + Math.cos(a) * ring;
                double z = base.z + Math.sin(a) * ring;
                level.sendParticles(ParticleTypes.WITCH, x, base.y + 0.4D, z, 2, 0.2D, 0.3D, 0.2D, 0.01D);
                if (i % 2 == 0) {
                    level.sendParticles(ParticleTypes.CHERRY_LEAVES, x, base.y + 1.2D, z, 1, 0.3D, 0.4D, 0.3D, 0.02D);
                }
            }
        }
        level.sendParticles(ParticleTypes.DRAGON_BREATH, base.x, base.y + 0.3D, base.z, 40, 3.0D, 0.2D, 3.0D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    /** Нож Джека: туман Лондона. Невидимость и скорость, преследователи теряют цель. */
    private boolean fog(ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1));
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0D), m -> m.getTarget() == player)) {
            mob.setTarget(null);
        }
        Vec3 base = player.position();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, base.x, base.y + 1.0D, base.z, 40, 0.8D, 0.8D, 0.8D, 0.02D);
        level.sendParticles(ParticleTypes.CLOUD, base.x, base.y + 0.6D, base.z, 30, 1.2D, 0.4D, 1.2D, 0.03D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.6F);
        return true;
    }

    /** Меч Спартака: крик мятежника. Владелец сильнее, враги рядом слабеют и отступают. */
    private boolean rebel(ServerLevel level, Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0D, 3.0D, 8.0D), t -> valid(t, player))) {
            if (e.distanceTo(player) > 8.0F) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
            e.knockback(0.6D, player.getX() - e.getX(), player.getZ() - e.getZ());
        }
        Vec3 base = player.position();
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, base.x, base.y + 2.0D, base.z, 8, 0.6D, 0.3D, 0.6D, 0.0D);
        for (int i = 0; i < 20; i++) {
            double a = Math.PI * 2.0D * i / 20.0D;
            level.sendParticles(ParticleTypes.CRIT, base.x + Math.cos(a) * 3.0D, base.y + 1.0D, base.z + Math.sin(a) * 3.0D, 2, 0.1D, 0.4D, 0.1D, 0.1D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }
}
