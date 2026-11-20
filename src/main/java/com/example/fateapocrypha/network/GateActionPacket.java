package com.example.fateapocrypha.network;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.world.CastleGate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент -> сервер: открыть (true) или закрыть (false) главные ворота замка. */
public class GateActionPacket {
    private final boolean open;

    public GateActionPacket(boolean open) {
        this.open = open;
    }

    public static void encode(GateActionPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.open);
    }

    public static GateActionPacket decode(FriendlyByteBuf buf) {
        return new GateActionPacket(buf.readBoolean());
    }

    public static void handle(GateActionPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.level().dimension().equals(ModDimensions.TRIFAS)) {
                return;
            }
            if (player.distanceToSqr(CastleGate.center().getX() + 0.5D, CastleGate.center().getY(),
                    CastleGate.center().getZ() + 0.5D) > 30.0D * 30.0D) {
                player.sendSystemMessage(Component.translatable("gate.fateapocrypha.too_far").withStyle(ChatFormatting.GRAY));
                return;
            }
            CastleGate.Result result = CastleGate.toggle(player.serverLevel(), p.open);
            String key = switch (result) {
                case OK -> p.open ? "gate.fateapocrypha.opening" : "gate.fateapocrypha.closing";
                case BUSY -> "gate.fateapocrypha.busy";
                case BLOCKED -> "gate.fateapocrypha.blocked";
                case ALREADY -> p.open ? "gate.fateapocrypha.already_open" : "gate.fateapocrypha.already_closed";
            };
            player.sendSystemMessage(Component.translatable(key)
                    .withStyle(result == CastleGate.Result.OK ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        });
        context.setPacketHandled(true);
    }
}
