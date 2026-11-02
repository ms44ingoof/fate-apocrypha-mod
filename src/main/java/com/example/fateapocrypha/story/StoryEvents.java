package com.example.fateapocrypha.story;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.ModItems;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public class StoryEvents {

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().is(ModItems.SUMMON_CATALYST.get())) {
            StoryManager.advanceTo(event.getEntity(), StoryChapter.SUMMONING);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo().equals(ModDimensions.TRIFAS)) {
            StoryManager.advanceTo(event.getEntity(), StoryChapter.MILLENNIUM_CASTLE);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        StoryManager.showObjective(event.getEntity());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        FateCommands.register(event.getDispatcher());
    }
}
