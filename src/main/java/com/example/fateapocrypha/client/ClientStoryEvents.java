package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.SkipDialoguePacket;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Во время сцен от третьего лица прячет руку и интерфейс Minecraft. */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, value = Dist.CLIENT)
public class ClientStoryEvents {
    private static final Set<String> HIDDEN_OVERLAYS = Set.of(
            "hotbar", "crosshair", "player_health", "armor_level", "food_level", "air_level",
            "experience_bar", "jump_bar", "vehicle_health", "item_name", "potion_icons",
            "chat_panel", "player_list", "boss_event_progress");

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientStoryState.clientTick();
            SealHud.tick();
            while (ClientKeys.JOURNAL.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null && !ClientStoryState.isCinematic()) {
                    mc.setScreen(new com.example.fateapocrypha.client.screen.QuestJournalScreen());
                }
            }
            while (ClientKeys.SKIP_DIALOGUE.consumeClick()) {
                if (Minecraft.getInstance().player != null && ClientStoryState.hasDialogue()) {
                    ModNetwork.CHANNEL.sendToServer(new SkipDialoguePacket());
                    ClientStoryState.clearDialogue();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientStoryState.isCinematic()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (ClientStoryState.isCinematic() && "minecraft".equals(event.getOverlay().id().getNamespace())
                && HIDDEN_OVERLAYS.contains(event.getOverlay().id().getPath())) {
            event.setCanceled(true);
        }
    }
}
