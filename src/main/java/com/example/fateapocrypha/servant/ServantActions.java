package com.example.fateapocrypha.servant;

import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.anim.FateAnimBus;
import com.example.fateapocrypha.entity.AstolfoEntity;
import com.example.fateapocrypha.entity.JeanneEntity;
import com.example.fateapocrypha.entity.MordredEntity;
import com.example.fateapocrypha.entity.ServantEntity;
import com.example.fateapocrypha.story.StoryManager;
import java.util.UUID;
import com.example.fateapocrypha.fx.FateFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Серверная логика меню Командного заклинания и призыва. */
public class ServantActions {
    public static final int ACTION_TELEPORT = 0;
    public static final int ACTION_BUFF = 1;
    public static final int ACTION_PHANTASM = 2;

    public static boolean hasItem(Player player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void consumeOne(Player player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) {
                stack.shrink(1);
                return;
            }
        }
    }

    public static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** Ищет живого Слугу игрока среди загруженных миров. */
    public static ServantEntity findServant(ServerPlayer player) {
        UUID id = StoryManager.getServantId(player);
        if (id == null || player.getServer() == null) {
            return null;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof ServantEntity servant && servant.isAlive()) {
                return servant;
            }
        }
        return null;
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    // ---------- Меню Командного заклинания ----------
    public static void handleSeal(ServerPlayer player, int action) {
        if (!StoryManager.hasSeals(player) && !player.getAbilities().instabuild
                && !hasItem(player, ModItems.COMMAND_SEAL.get())) {
            message(player, "message.fateapocrypha.no_seal");
            return;
        }
        ServantEntity servant = findServant(player);
        if (servant == null) {
            message(player, "message.fateapocrypha.no_servant");
            return;
        }
        if (player.level() instanceof ServerLevel fxLevel) {
            sealFx(fxLevel, player, servant, action);
        }
        switch (action) {
            case ACTION_TELEPORT -> teleportServant(player, servant);
            case ACTION_BUFF -> servant.useBuff(player);
            case ACTION_PHANTASM -> servant.usePhantasm(player);
            default -> { }
        }
    }

    private static void teleportServant(ServerPlayer player, ServantEntity servant) {
        if (servant.level() != player.level()) {
            message(player, "message.fateapocrypha.other_dimension");
            return;
        }
        int remaining = servant.getTeleportRemaining();
        if (remaining > 0) {
            servant.sendCooldownMessage(player, remaining);
            return;
        }
        servant.startTeleportCooldown();
        servant.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        servant.getNavigation().stop();
        servant.setDeltaMovement(Vec3.ZERO);
        servant.fallDistance = 0.0F;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY(0.5D), player.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.2D);
        }
        message(player, "message.fateapocrypha.teleported");
    }

    // ---------- Призыв ----------
    public static void handleSummon(ServerPlayer player, int typeId) {
        ServantType type = ServantType.byId(typeId);
        if (type == null) {
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        if (!creative && !hasItem(player, ModItems.SUMMON_CATALYST.get())) {
            message(player, "message.fateapocrypha.no_catalyst");
            return;
        }
        if (findServant(player) != null) {
            message(player, "message.fateapocrypha.has_servant");
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ServantEntity servant = type.entityType().create(level);
        if (servant == null) {
            return;
        }

        Vec3 look = player.getLookAngle();
        servant.moveTo(player.getX() + look.x * 2.0D, player.getY(), player.getZ() + look.z * 2.0D,
                player.getYRot() + 180.0F, 0.0F);
        servant.tame(player);
        servant.setPersistenceRequired();
        ItemStack weapon = type.weapon();
        if (!weapon.isEmpty()) {
            servant.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            servant.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        level.addFreshEntity(servant);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(servant.getX(), servant.getY(), servant.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        summonFx(level, servant);
        StoryManager.setServantId(player, servant.getUUID());
        if (!creative) {
            consumeOne(player, ModItems.SUMMON_CATALYST.get());
        }
        player.sendSystemMessage(Component.translatable("message.fateapocrypha.summoned",
                Component.translatable(type.nameKey())));
    }

    /** Призывает Слугу игроку без катализатора (для сюжета). */
    public static ServantEntity spawnServant(ServerPlayer player, ServantType type) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        ServantEntity servant = type.entityType().create(level);
        if (servant == null) {
            return null;
        }
        Vec3 look = player.getLookAngle();
        servant.moveTo(player.getX() + look.x * 2.0D, player.getY(), player.getZ() + look.z * 2.0D,
                player.getYRot() + 180.0F, 0.0F);
        servant.tame(player);
        servant.setPersistenceRequired();
        ItemStack weapon = type.weapon();
        if (!weapon.isEmpty()) {
            servant.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            servant.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        level.addFreshEntity(servant);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(servant.getX(), servant.getY(), servant.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        summonFx(level, servant);
        StoryManager.setServantId(player, servant.getUUID());
        return servant;
    }

    /** Эффект печати: у каждого действия свой рисунок. */
    private static void sealFx(ServerLevel level, ServerPlayer player, ServantEntity servant, int action) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        // Мастер и Слуга играют согласованные анимации: клип PAL у игрока и жест у Слуги (GeckoLib-рендерер)
        switch (action) {
            case ACTION_TELEPORT -> FateAnimBus.together(player, "seal", servant, "cast", 30);
            case ACTION_BUFF -> FateAnimBus.together(player, "bless", servant, "pray", 40);
            default -> FateAnimBus.together(player, "beam", servant, "charge", 40);
        }
        switch (action) {
            case ACTION_TELEPORT -> {   // фиолетовая шестерня и спираль, как перенос через пространство
                FateFx.circle(level, px, py, pz, FateFx.VIOLET, 2.4F, 30, FateFx.STYLE_GEAR);
                FateFx.helix(level, px, py, pz, FateFx.VIOLET, 3.2F, 0.9D, 22);
                FateFx.burst(level, servant.getX(), servant.getY() + 1.0D, servant.getZ(), FateFx.VIOLET, 2.5F, 14);
            }
            case ACTION_BUFF -> {   // золотой лотос и столб: благословение
                FateFx.circle(level, px, py, pz, FateFx.GOLD, 3.0F, 44, FateFx.STYLE_LOTUS);
                FateFx.pillar(level, px, py, pz, 0xFFE79A, 7.0F, 18);
                FateFx.helix(level, servant.getX(), servant.getY(), servant.getZ(), 0xFFE79A, 3.0F, 0.8D, 24);
            }
            default -> {   // Фантазм: солнце, крылья, ливень и тряска
                FateFx.sun(level, servant.getX(), servant.getY(), servant.getZ(), FateFx.GOLD, 8.0F, 40);
                FateFx.circle(level, px, py, pz, FateFx.RED, 3.0F, 40, FateFx.STYLE_OCTA);
                FateFx.wings(level, servant.getX(), servant.getY(), servant.getZ(), servant.getLookAngle().x,
                        servant.getLookAngle().z, 0xFFF1C2, 2.6F, 28);
                FateFx.rain(level, servant.getX(), servant.getY(), servant.getZ(), 0xFFD24A, 7.0F, 36);
                FateFx.shake(level, servant.getX(), servant.getY(), servant.getZ(), 30.0D, 0.7F, 14);
            }
        }
    }

    /** Эффект призыва: у каждого героя свой рисунок круга и свой цвет. */
    private static void summonFx(ServerLevel level, ServantEntity servant) {
        double x = servant.getX();
        double y = servant.getY();
        double z = servant.getZ();
        if (servant instanceof AstolfoEntity) {          // розовый лотос и крылья гиппогрифа
            FateFx.circle(level, x, y, z, 0xFF9CCB, 3.4F, 64, FateFx.STYLE_LOTUS);
            FateFx.wings(level, x, y, z, 0.0D, 1.0D, 0xFFD6EA, 2.8F, 40);
            FateFx.rain(level, x, y, z, 0xFFC4E0, 3.5F, 50);
        } else if (servant instanceof JeanneEntity) {    // золотое солнце и белый столб
            FateFx.sun(level, x, y, z, 0xFFE9A0, 4.2F, 56);
            FateFx.circle(level, x, y, z, 0xFFF1C2, 3.0F, 64, FateFx.STYLE_HEX);
            FateFx.pillar(level, x, y, z, 0xFFFFFF, 16.0F, 26);
        } else if (servant instanceof MordredEntity) {   // красная октаграмма и молнии
            FateFx.circle(level, x, y, z, FateFx.RED, 3.4F, 64, FateFx.STYLE_OCTA);
            FateFx.lightning(level, new net.minecraft.world.phys.Vec3(x, y + 14.0D, z),
                    new net.minecraft.world.phys.Vec3(x, y, z), 0xFF7A7A, 0.7F, 12);
            FateFx.burst(level, x, y + 1.0D, z, FateFx.RED, 3.2F, 18);
        } else {                                         // Семирамида: шестерня и спираль яда
            FateFx.circle(level, x, y, z, FateFx.VIOLET, 3.6F, 64, FateFx.STYLE_GEAR);
            FateFx.helix(level, x, y, z, FateFx.VIOLET, 5.0F, 1.4D, 44);
            FateFx.rain(level, x, y, z, 0xD6B8FF, 3.5F, 50);
        }
        FateFx.pillar(level, x, y, z, 0xBFE6FF, 14.0F, 24);
        FateFx.shake(level, x, y, z, 24.0D, 0.6F, 14);
    }
}
