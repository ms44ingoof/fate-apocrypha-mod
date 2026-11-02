package com.example.fateapocrypha.network;

import com.example.fateapocrypha.servant.ServantActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент -> сервер: выбор Слуги в меню катализатора. */
public class SummonPacket {
    private final int typeId;

    public SummonPacket(int typeId) {
        this.typeId = typeId;
    }

    public static void encode(SummonPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.typeId);
    }

    public static SummonPacket decode(FriendlyByteBuf buf) {
        return new SummonPacket(buf.readVarInt());
    }

    public static void handle(SummonPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServantActions.handleSummon(player, packet.typeId);
            }
        });
        context.setPacketHandled(true);
    }
}
