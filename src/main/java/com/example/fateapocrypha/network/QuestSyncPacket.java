package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientQuestState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: текущая глава и началась ли её сцена (для журнала и метки на экране). */
public class QuestSyncPacket {
    private final int chapter;
    private final boolean started;
    private final boolean seals;

    public QuestSyncPacket(int chapter, boolean started, boolean seals) {
        this.chapter = chapter;
        this.started = started;
        this.seals = seals;
    }

    public static void encode(QuestSyncPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.chapter);
        buf.writeBoolean(p.started);
        buf.writeBoolean(p.seals);
    }

    public static QuestSyncPacket decode(FriendlyByteBuf buf) {
        return new QuestSyncPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(QuestSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientQuestState.update(p.chapter, p.started, p.seals)));
        context.setPacketHandled(true);
    }
}
