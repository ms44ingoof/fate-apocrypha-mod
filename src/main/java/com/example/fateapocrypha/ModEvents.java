package com.example.fateapocrypha;

import com.example.fateapocrypha.entity.AstolfoEntity;
import com.example.fateapocrypha.entity.JeanneEntity;
import com.example.fateapocrypha.entity.MordredEntity;
import com.example.fateapocrypha.entity.SemiramisEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ASTOLFO.get(), AstolfoEntity.createAttributes().build());
        event.put(ModEntities.MORDRED.get(), MordredEntity.createAttributes().build());
        event.put(ModEntities.JEANNE.get(), JeanneEntity.createAttributes().build());
        event.put(ModEntities.SEMIRAMIS.get(), SemiramisEntity.createAttributes().build());
    }
}
