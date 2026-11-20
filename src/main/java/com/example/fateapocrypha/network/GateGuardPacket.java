package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientGate;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: открыть окно привратника (состояние ворот: 0 закрыты, 1 открыты, 2 движутся). */
public class GateGuardPacket {
    private final int state;

    public GateGuardPacket(int state) {
        this.state = state;
    }

    public static void encode(GateGuardPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.state);
    }

    public static GateGuardPacket decode(FriendlyByteBuf buf) {
        return new GateGuardPacket(buf.readVarInt());
    }

    public static void handle(GateGuardPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientGate.open(p.state)));
        context.setPacketHandled(true);
    }
}
