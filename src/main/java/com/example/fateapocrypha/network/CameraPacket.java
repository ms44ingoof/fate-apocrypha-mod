package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientStoryState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: смотреть глазами сущности-камеры (entityId >= 0) или вернуться к игроку (-1). */
public class CameraPacket {
    private final int entityId;
    private final boolean cinematic;

    public CameraPacket(int entityId, boolean cinematic) {
        this.entityId = entityId;
        this.cinematic = cinematic;
    }

    public static void encode(CameraPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId);
        buf.writeBoolean(p.cinematic);
    }

    public static CameraPacket decode(FriendlyByteBuf buf) {
        return new CameraPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(CameraPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStoryState.setCamera(p.entityId, p.cinematic)));
        context.setPacketHandled(true);
    }
}
