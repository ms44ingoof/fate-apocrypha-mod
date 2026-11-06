package com.example.fateapocrypha.network;

import com.example.fateapocrypha.scene.SceneManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент -> сервер: пропустить текущую реплику сцены. */
public class SkipDialoguePacket {
    public static void encode(SkipDialoguePacket packet, FriendlyByteBuf buf) {
    }

    public static SkipDialoguePacket decode(FriendlyByteBuf buf) {
        return new SkipDialoguePacket();
    }

    public static void handle(SkipDialoguePacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SceneManager.requestSkip(player);
            }
        });
        context.setPacketHandled(true);
    }
}
