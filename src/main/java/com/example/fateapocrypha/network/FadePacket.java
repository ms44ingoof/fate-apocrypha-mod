package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientStoryState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: затемнение экрана (появление, пауза, исчезновение; в тиках). */
public class FadePacket {
    private final int in;
    private final int hold;
    private final int out;

    public FadePacket(int in, int hold, int out) {
        this.in = in;
        this.hold = hold;
        this.out = out;
    }

    public static void encode(FadePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.in);
        buf.writeVarInt(p.hold);
        buf.writeVarInt(p.out);
    }

    public static FadePacket decode(FriendlyByteBuf buf) {
        return new FadePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FadePacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStoryState.startFade(p.in, p.hold, p.out)));
        context.setPacketHandled(true);
    }
}
