package com.example.fateapocrypha.story;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.world.SideBoards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Доски объявлений: нажатие на лектор открывает работу с заданиями, а рядом с игроком доски появляются в мире. */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public class SideQuestEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!level.dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        SideBoard board = SideBoard.at(event.getPos());
        if (board == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            SideQuestManager.interact(player, board);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0 || !player.level().dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        if (player.tickCount % 100 == 0) {
            SideBoards.ensureNear(player.serverLevel(), player);
        }
        SideQuestManager.tick(player);
    }
}
