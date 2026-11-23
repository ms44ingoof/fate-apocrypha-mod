package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.pal.PalBridge;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиенты: игрок с этим UUID проигрывает клип PAL с этим именем. */
public class PlayerAnimPacket {
    private final UUID player;
    private final String name;

    public PlayerAnimPacket(UUID player, String name) {
        this.player = player;
        this.name = name;
    }

    public static void encode(PlayerAnimPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.player);
        buf.writeUtf(p.name, 64);
    }

    public static PlayerAnimPacket decode(FriendlyByteBuf buf) {
        return new PlayerAnimPacket(buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(PlayerAnimPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PalBridge.play(p.player, p.name)));
        context.setPacketHandled(true);
    }
}
