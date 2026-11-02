package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    private static ResourceLocation skin(String name) {
        return new ResourceLocation(FateApocrypha.MODID, "textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // третий параметр: true = тонкие руки (Alex), false = обычные (Steve)
        event.registerEntityRenderer(ModEntities.ASTOLFO.get(),
                ctx -> new ServantRenderer<>(ctx, skin("astolfo"), true));
        event.registerEntityRenderer(ModEntities.MORDRED.get(),
                ctx -> new ServantRenderer<>(ctx, skin("mordred"), false));
        event.registerEntityRenderer(ModEntities.JEANNE.get(),
                ctx -> new ServantRenderer<>(ctx, skin("jeanne"), true));
        event.registerEntityRenderer(ModEntities.SEMIRAMIS.get(),
                ctx -> new ServantRenderer<>(ctx, skin("semiramis"), false));
    }
}
