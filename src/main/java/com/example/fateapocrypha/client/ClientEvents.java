package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
        event.registerEntityRenderer(ModEntities.STORY_NPC.get(), StoryNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.STORY_CAMERA.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.SKIP_DIALOGUE);
        event.register(ClientKeys.JOURNAL);
        event.register(ClientKeys.SEAL_TELEPORT);
        event.register(ClientKeys.SEAL_BUFF);
        event.register(ClientKeys.SEAL_PHANTASM);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("story_overlay",
                (gui, graphics, partialTick, width, height) -> ClientStoryState.render(graphics, width, height));
        event.registerAboveAll("seal_hud",
                (gui, graphics, partialTick, width, height) -> SealHud.render(graphics, width, height));
        event.registerAboveAll("quest_hud",
                (gui, graphics, partialTick, width, height) -> QuestHud.render(graphics, width, height, partialTick));
    }
}
