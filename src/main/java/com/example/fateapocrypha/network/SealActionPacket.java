package com.example.fateapocrypha.network;

import com.example.fateapocrypha.servant.ServantActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент -> сервер: действие из меню Командного заклинания. */
public class SealActionPacket {
    private final int action;

    public SealActionPacket(int action) {
        this.action = action;
    }

    public static void encode(SealActionPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.action);
    }

    public static SealActionPacket decode(FriendlyByteBuf buf) {
        return new SealActionPacket(buf.readVarInt());
    }

    public static void handle(SealActionPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServantActions.handleSeal(player, packet.action);
            }
        });
        context.setPacketHandled(true);
    }
}
