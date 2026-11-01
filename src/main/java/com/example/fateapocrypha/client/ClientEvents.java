package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ASTOLFO.get(), AstolfoRenderer::new);
    }
}
