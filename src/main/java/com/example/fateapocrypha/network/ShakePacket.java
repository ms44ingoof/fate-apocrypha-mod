package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.fx.ClientFx;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: тряска камеры (сила в градусах, длительность в тиках). */
public class ShakePacket {
    private final float power;
    private final int ticks;

    public ShakePacket(float power, int ticks) {
        this.power = power;
        this.ticks = ticks;
    }

    public static void encode(ShakePacket p, FriendlyByteBuf buf) {
        buf.writeFloat(p.power);
        buf.writeVarInt(p.ticks);
    }

    public static ShakePacket decode(FriendlyByteBuf buf) {
        return new ShakePacket(buf.readFloat(), buf.readVarInt());
    }

    public static void handle(ShakePacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.shake(p.power, p.ticks)));
        context.setPacketHandled(true);
    }
}
