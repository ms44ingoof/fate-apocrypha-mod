package com.example.fateapocrypha.item;

import com.example.fateapocrypha.ModEntities;
import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.entity.AstolfoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryManager;

/** Катализатор: ПКМ по блоку призывает Астольфо как Слугу игрока. */
public class SummonCatalystItem extends Item {
    public SummonCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        AstolfoEntity servant = ModEntities.ASTOLFO.get().create(level);
        if (servant == null) {
            return InteractionResult.FAIL;
        }
        servant.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        servant.tame(player);
        servant.setPersistenceRequired();
        servant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        servant.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        level.addFreshEntity(servant);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        player.sendSystemMessage(Component.translatable("message.fateapocrypha.summoned"));

        // Первый призыв: награда - Командные заклинания, и сюжет идёт дальше
        if (StoryManager.getChapter(player).ordinal() < StoryChapter.ROMANIA.ordinal()) {
            player.addItem(new ItemStack(ModItems.COMMAND_SEAL.get()));
            player.sendSystemMessage(Component.translatable("message.fateapocrypha.seal_awakened"));
            StoryManager.advanceTo(player, StoryChapter.ROMANIA);
        }
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
