package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientSideQuests;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: состояние каждого побочного задания (порядок SideQuest.values()). */
public class SideQuestSyncPacket {
    private final byte[] states;

    public SideQuestSyncPacket(byte[] states) {
        this.states = states;
    }

    public static void encode(SideQuestSyncPacket p, FriendlyByteBuf buf) {
        buf.writeByteArray(p.states);
    }

    public static SideQuestSyncPacket decode(FriendlyByteBuf buf) {
        return new SideQuestSyncPacket(buf.readByteArray());
    }

    public static void handle(SideQuestSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSideQuests.update(p.states)));
        context.setPacketHandled(true);
    }
}
