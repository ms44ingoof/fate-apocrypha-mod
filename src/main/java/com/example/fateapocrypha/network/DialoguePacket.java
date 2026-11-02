package com.example.fateapocrypha.network;

import com.example.fateapocrypha.client.ClientStoryState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер -> клиент: показать реплику в рамке внизу экрана. */
public class DialoguePacket {
    private final Component speaker;
    private final Component text;
    private final int ticks;

    public DialoguePacket(Component speaker, Component text, int ticks) {
        this.speaker = speaker;
        this.text = text;
        this.ticks = ticks;
    }

    public static void encode(DialoguePacket p, FriendlyByteBuf buf) {
        buf.writeComponent(p.speaker);
        buf.writeComponent(p.text);
        buf.writeVarInt(p.ticks);
    }

    public static DialoguePacket decode(FriendlyByteBuf buf) {
        return new DialoguePacket(buf.readComponent(), buf.readComponent(), buf.readVarInt());
    }

    public static void handle(DialoguePacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStoryState.showDialogue(p.speaker, p.text, p.ticks)));
        context.setPacketHandled(true);
    }
}
