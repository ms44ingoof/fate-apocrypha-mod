package com.example.fateapocrypha.servant;

import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.entity.ServantEntity;
import com.example.fateapocrypha.story.StoryManager;
import java.util.UUID;
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
        if (!hasItem(player, ModItems.COMMAND_SEAL.get())) {
            message(player, "message.fateapocrypha.no_seal");
            return;
        }
        ServantEntity servant = findServant(player);
        if (servant == null) {
            message(player, "message.fateapocrypha.no_servant");
            return;
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
        StoryManager.setServantId(player, servant.getUUID());
        return servant;
    }
}
