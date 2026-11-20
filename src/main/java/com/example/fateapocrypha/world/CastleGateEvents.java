package com.example.fateapocrypha.world;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.network.GateGuardPacket;
import com.example.fateapocrypha.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Нажатие на привратника открывает окно управления воротами вместо торговли. */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public final class CastleGateEvents {
    private CastleGateEvents() {
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!CastleGate.isGuard(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new GateGuardPacket(CastleGate.state(player.serverLevel())));
        }
    }
}
