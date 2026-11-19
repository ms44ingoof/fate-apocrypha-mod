package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientBoard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: открыть окно доски объявлений (номер SideBoard). */
public class OpenBoardPacket {
    private final int board;

    public OpenBoardPacket(int board) {
        this.board = board;
    }

    public static void encode(OpenBoardPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.board);
    }

    public static OpenBoardPacket decode(FriendlyByteBuf buf) {
        return new OpenBoardPacket(buf.readVarInt());
    }

    public static void handle(OpenBoardPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBoard.open(p.board)));
        context.setPacketHandled(true);
    }
}
