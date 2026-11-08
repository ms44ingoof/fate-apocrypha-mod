package com.example.fateapocrypha.story;

import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.QuestSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** Отправляет клиенту состояние заданий. */
public final class QuestSync {
    private QuestSync() {
    }

    public static void send(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new QuestSyncPacket(StoryManager.getChapter(player).ordinal(), StoryManager.isStarted(player)));
    }
}
