package com.example.fateapocrypha.story;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.servant.ServantActions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public class StoryEvents {

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo().equals(ModDimensions.TRIFAS)) {
            StoryManager.advanceTo(event.getEntity(), StoryChapter.MILLENNIUM_CASTLE);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!StoryManager.hasReceivedKit(player)) {
            ServantActions.give(player, new ItemStack(ModItems.COMMAND_SEAL.get()));
            ServantActions.give(player, new ItemStack(ModItems.SUMMON_CATALYST.get()));
            StoryManager.setReceivedKit(player);
            player.sendSystemMessage(Component.translatable("message.fateapocrypha.kit"));
        }
        StoryManager.showObjective(player);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        FateCommands.register(event.getDispatcher());
    }
}
