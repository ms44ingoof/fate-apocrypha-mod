package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.fx.ClientFx;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: нарисовать собственный эффект (круг, кольцо, столб, луч, взмах, всплеск). */
public class FxPacket {
    private final int kind;
    private final double x;
    private final double y;
    private final double z;
    private final double ax;
    private final double ay;
    private final double az;
    private final int color;
    private final float size;
    private final int ticks;

    public FxPacket(int kind, double x, double y, double z, double ax, double ay, double az, int color, float size, int ticks) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.color = color;
        this.size = size;
        this.ticks = ticks;
    }

    public static void encode(FxPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.kind);
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
        buf.writeDouble(p.ax);
        buf.writeDouble(p.ay);
        buf.writeDouble(p.az);
        buf.writeInt(p.color);
        buf.writeFloat(p.size);
        buf.writeVarInt(p.ticks);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(FxPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientFx.add(p.kind, p.x, p.y, p.z, p.ax, p.ay, p.az, p.color, p.size, p.ticks)));
        context.setPacketHandled(true);
    }
}
