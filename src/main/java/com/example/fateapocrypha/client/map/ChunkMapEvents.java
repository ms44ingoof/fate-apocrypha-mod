package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.FateApocrypha;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Подключает сканирование карты к тикам клиента и сохраняет карту при выходе из мира. */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChunkMapEvents {
    private ChunkMapEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ChunkMap.tick();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ChunkMap.shutdown();
    }
}
